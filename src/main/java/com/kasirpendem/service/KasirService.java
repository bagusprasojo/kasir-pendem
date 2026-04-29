package com.kasirpendem.service;

import com.kasirpendem.config.AppConfig;
import com.kasirpendem.db.Database;
import com.kasirpendem.model.Role;
import com.kasirpendem.model.User;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

public class KasirService {

    public Optional<User> login(String username, String password) {
        String sql = "SELECT id, username, role FROM users WHERE username=? AND password_hash=SHA2(?,256)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(rs.getLong("id"), rs.getString("username"), Role.valueOf(rs.getString("role"))));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal login", e);
        }
        return Optional.empty();
    }

    public List<String> listBarang() {
        List<String> rows = new ArrayList<>();
        String sql = "SELECT id, kode, nama, stok, harga_jual FROM barang ORDER BY nama";
        try (Connection conn = Database.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(rs.getLong("id") + " | " + rs.getString("kode") + " | " + rs.getString("nama") + " | stok=" + rs.getBigDecimal("stok") + " | harga=" + rs.getBigDecimal("harga_jual"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    public List<String> listBarangPos() {
        List<String> rows = new ArrayList<>();
        String sql = "SELECT id, kode, COALESCE(barcode,''), nama, stok, harga_jual, ppn_persen FROM barang ORDER BY nama";
        try (Connection conn = Database.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(rs.getLong(1) + "|" + rs.getString(2) + "|" + rs.getString(3) + "|" + rs.getString(4) + "|" + rs.getBigDecimal(5) + "|" + rs.getBigDecimal(6) + "|" + rs.getBigDecimal(7));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    public List<String> searchSantri(String keyword) {
        List<String> rows = new ArrayList<>();
        String k = keyword == null ? "" : keyword.trim();
        String sql = "SELECT id, nis, nama, kelas, saldo FROM santri WHERE aktif=1 AND (nis LIKE ? OR nama LIKE ?) ORDER BY nama LIMIT 50";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + k + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(rs.getLong("id") + " | " + rs.getString("nis") + " | " + rs.getString("nama") + " | " + rs.getString("kelas") + " | saldo=" + rs.getBigDecimal("saldo"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    public String processTopup(long santriId, BigDecimal amount, long createdBy) {
        BigDecimal min = BigDecimal.valueOf(AppConfig.getInt("app.topup.min"));
        BigDecimal max = BigDecimal.valueOf(AppConfig.getInt("app.topup.max"));
        if (amount.compareTo(min) < 0 || amount.compareTo(max) > 0) {
            return "Nominal topup di luar batas konfigurasi";
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal saldo = currentSaldo(conn, santriId);
                BigDecimal saldoBaru = saldo.add(amount);
                updateSaldo(conn, santriId, saldoBaru);
                insertWallet(conn, santriId, "TOPUP", amount, saldoBaru, null, null, null, createdBy);
                conn.commit();
                return "Topup sukses. Saldo sekarang: " + saldoBaru;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            return "Topup gagal: " + e.getMessage();
        }
    }

    public String reverseTopup(long walletTxId, String reason, long adminId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String q = "SELECT santri_id, nominal, tipe FROM wallet_transaction WHERE id=? FOR UPDATE";
                long santriId;
                BigDecimal nominal;
                String tipe;
                try (PreparedStatement ps = conn.prepareStatement(q)) {
                    ps.setLong(1, walletTxId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return "Transaksi wallet tidak ditemukan";
                        }
                        santriId = rs.getLong("santri_id");
                        nominal = rs.getBigDecimal("nominal");
                        tipe = rs.getString("tipe");
                    }
                }
                if (!"TOPUP".equals(tipe)) {
                    return "Hanya transaksi TOPUP yang bisa dibatalkan";
                }
                BigDecimal saldo = currentSaldo(conn, santriId);
                BigDecimal saldoBaru = saldo.subtract(nominal);
                if (saldoBaru.compareTo(BigDecimal.ZERO) < 0) {
                    return "Pembatalan ditolak: saldo akan minus";
                }
                updateSaldo(conn, santriId, saldoBaru);
                insertWallet(conn, santriId, "TOPUP_REVERSAL", nominal.negate(), saldoBaru, null, reason, adminId, adminId);
                conn.commit();
                return "Pembatalan topup berhasil";
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            return "Pembatalan topup gagal: " + e.getMessage();
        }
    }

    public String processSale(long kasirId, Long santriId, List<SaleItem> items, BigDecimal cashPay, BigDecimal depositPay) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal subtotal = BigDecimal.ZERO;
                BigDecimal totalDiskon = BigDecimal.ZERO;
                BigDecimal totalPpn = BigDecimal.ZERO;

                for (SaleItem item : items) {
                    lockAndValidateStock(conn, item.barangId(), item.qty());
                    BigDecimal line = item.harga().multiply(item.qty());
                    BigDecimal ppn = line.subtract(item.diskon()).multiply(item.ppnPersen()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    subtotal = subtotal.add(line);
                    totalDiskon = totalDiskon.add(item.diskon());
                    totalPpn = totalPpn.add(ppn);
                }

                BigDecimal grand = subtotal.subtract(totalDiskon).add(totalPpn);
                if (cashPay.add(depositPay).compareTo(grand) < 0) {
                    return "Pembayaran kurang dari grand total";
                }

                if (santriId != null && depositPay.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal saldo = currentSaldo(conn, santriId);
                    if (saldo.compareTo(depositPay) < 0) {
                        return "Saldo deposit tidak cukup";
                    }
                    BigDecimal saldoBaru = saldo.subtract(depositPay);
                    updateSaldo(conn, santriId, saldoBaru);
                }

                String noTrx = nextTrxNo(conn);
                long trxId;
                String trxSql = "INSERT INTO transaksi(no_trx,tanggal,subtotal,total_diskon,total_ppn,grand_total,bayar_tunai,bayar_deposit,kasir_id,santri_id) VALUES(?,NOW(),?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(trxSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, noTrx);
                    ps.setBigDecimal(2, subtotal);
                    ps.setBigDecimal(3, totalDiskon);
                    ps.setBigDecimal(4, totalPpn);
                    ps.setBigDecimal(5, grand);
                    ps.setBigDecimal(6, cashPay);
                    ps.setBigDecimal(7, depositPay);
                    ps.setLong(8, kasirId);
                    if (santriId == null) ps.setNull(9, Types.BIGINT); else ps.setLong(9, santriId);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        trxId = rs.getLong(1);
                    }
                }

                String detailSql = "INSERT INTO transaksi_detail(transaksi_id,barang_id,qty,harga_jual,diskon_item,ppn_persen,total) VALUES(?,?,?,?,?,?,?)";
                for (SaleItem item : items) {
                    try (PreparedStatement ps = conn.prepareStatement(detailSql)) {
                        BigDecimal total = item.harga().multiply(item.qty()).subtract(item.diskon());
                        ps.setLong(1, trxId);
                        ps.setLong(2, item.barangId());
                        ps.setBigDecimal(3, item.qty());
                        ps.setBigDecimal(4, item.harga());
                        ps.setBigDecimal(5, item.diskon());
                        ps.setBigDecimal(6, item.ppnPersen());
                        ps.setBigDecimal(7, total);
                        ps.executeUpdate();
                    }
                    updateStock(conn, item.barangId(), item.qty().negate(), "OUT", "PENJUALAN", "Penjualan " + noTrx, kasirId);
                }

                if (santriId != null && depositPay.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal saldoNow = currentSaldo(conn, santriId);
                    insertWallet(conn, santriId, "PAYMENT", depositPay.negate(), saldoNow, noTrx, "Pembayaran transaksi", null, kasirId);
                }

                conn.commit();
                return "Transaksi sukses: " + noTrx;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            return "Transaksi gagal: " + e.getMessage();
        }
    }

    public String inputPembelian(long supplierId, List<PurchaseItem> items, long userId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal total = BigDecimal.ZERO;
                for (PurchaseItem i : items) {
                    total = total.add(i.hargaBeli().multiply(i.qty()));
                }
                String no = "BELI-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
                long pembelianId;
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO pembelian(no_beli,supplier_id,tanggal,total,created_by) VALUES(?,?,NOW(),?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, no);
                    ps.setLong(2, supplierId);
                    ps.setBigDecimal(3, total);
                    ps.setLong(4, userId);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        pembelianId = rs.getLong(1);
                    }
                }
                for (PurchaseItem i : items) {
                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO pembelian_detail(pembelian_id,barang_id,qty,harga_beli) VALUES(?,?,?,?)")) {
                        ps.setLong(1, pembelianId);
                        ps.setLong(2, i.barangId());
                        ps.setBigDecimal(3, i.qty());
                        ps.setBigDecimal(4, i.hargaBeli());
                        ps.executeUpdate();
                    }
                    updateAvgCost(conn, i.barangId(), i.qty(), i.hargaBeli());
                    updateStock(conn, i.barangId(), i.qty(), "IN", "KULAKAN", "Kulakan " + no, userId);
                }
                conn.commit();
                return "Kulakan tersimpan: " + no;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            return "Kulakan gagal: " + e.getMessage();
        }
    }

    public String stockOpname(List<StockOpnameItem> items, long userId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (StockOpnameItem item : items) {
                    BigDecimal current = readStock(conn, item.barangId());
                    BigDecimal selisih = item.stokFisik().subtract(current);
                    if (selisih.compareTo(BigDecimal.ZERO) != 0) {
                        updateStock(conn, item.barangId(), selisih, "ADJUSTMENT", "STOCK_OPNAME", "Stock opname", userId);
                    }
                }
                conn.commit();
                return "Stock opname berhasil";
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            return "Stock opname gagal: " + e.getMessage();
        }
    }

    public String stockOutNonSales(long barangId, BigDecimal qty, String kategori, String note, long userId) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return "Qty harus lebih dari 0";
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                lockAndValidateStock(conn, barangId, qty);
                updateStock(conn, barangId, qty.negate(), "OUT", kategori, note, userId);
                conn.commit();
                return "Stok keluar non-penjualan tercatat";
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            return "Gagal mencatat stok keluar: " + e.getMessage();
        }
    }

    public List<String> reportPenjualan(LocalDate from, LocalDate to) {
        List<String> rows = new ArrayList<>();
        String sql = "SELECT no_trx,tanggal,grand_total,bayar_tunai,bayar_deposit FROM transaksi WHERE DATE(tanggal) BETWEEN ? AND ? ORDER BY tanggal";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(rs.getString(1) + ";" + rs.getTimestamp(2) + ";" + rs.getBigDecimal(3) + ";" + rs.getBigDecimal(4) + ";" + rs.getBigDecimal(5));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    public List<String> reportDeposit(LocalDate from, LocalDate to) {
        List<String> rows = new ArrayList<>();
        String sql = "SELECT santri_id,tipe,nominal,saldo_setelah,created_at FROM wallet_transaction WHERE DATE(created_at) BETWEEN ? AND ? ORDER BY created_at";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(rs.getLong(1) + ";" + rs.getString(2) + ";" + rs.getBigDecimal(3) + ";" + rs.getBigDecimal(4) + ";" + rs.getTimestamp(5));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    public List<String> reportStokMinimum() {
        List<String> rows = new ArrayList<>();
        String sql = "SELECT kode,nama,stok,stok_min FROM barang WHERE stok <= stok_min ORDER BY nama";
        try (Connection conn = Database.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(rs.getString(1) + ";" + rs.getString(2) + ";" + rs.getBigDecimal(3) + ";" + rs.getBigDecimal(4));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    public String createBarang(String kode, String barcode, String nama, String satuan, BigDecimal hargaJual, BigDecimal ppnPersen, BigDecimal stokMin) {
        String sql = "INSERT INTO barang(kode,barcode,nama,satuan,harga_jual,ppn_persen,stok_min) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.setString(2, barcode);
            ps.setString(3, nama);
            ps.setString(4, satuan);
            ps.setBigDecimal(5, hargaJual);
            ps.setBigDecimal(6, ppnPersen);
            ps.setBigDecimal(7, stokMin);
            ps.executeUpdate();
            return "Barang berhasil ditambahkan";
        } catch (SQLException e) {
            return "Gagal tambah barang: " + e.getMessage();
        }
    }

    public List<String> listBarangMaster(String keyword) {
        List<String> rows = new ArrayList<>();
        String k = keyword == null ? "" : keyword.trim();
        String sql = "SELECT id,kode,COALESCE(barcode,''),nama,satuan,harga_jual,ppn_persen,stok,stok_min FROM barang " +
                "WHERE (?='' OR kode LIKE ? OR barcode LIKE ? OR nama LIKE ?) ORDER BY nama";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + k + "%";
            ps.setString(1, k);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(rs.getLong(1) + "|" + rs.getString(2) + "|" + rs.getString(3) + "|" + rs.getString(4) + "|" +
                            rs.getString(5) + "|" + rs.getBigDecimal(6) + "|" + rs.getBigDecimal(7) + "|" + rs.getBigDecimal(8) + "|" + rs.getBigDecimal(9));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    public String updateBarang(long id, String kode, String barcode, String nama, String satuan, BigDecimal hargaJual, BigDecimal ppnPersen, BigDecimal stokMin) {
        String sql = "UPDATE barang SET kode=?, barcode=?, nama=?, satuan=?, harga_jual=?, ppn_persen=?, stok_min=? WHERE id=?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.setString(2, barcode);
            ps.setString(3, nama);
            ps.setString(4, satuan);
            ps.setBigDecimal(5, hargaJual);
            ps.setBigDecimal(6, ppnPersen);
            ps.setBigDecimal(7, stokMin);
            ps.setLong(8, id);
            int n = ps.executeUpdate();
            return n > 0 ? "Barang berhasil diupdate" : "Barang tidak ditemukan";
        } catch (SQLException e) {
            return "Gagal update barang: " + e.getMessage();
        }
    }

    public String deleteBarang(long id) {
        String sql = "DELETE FROM barang WHERE id=?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int n = ps.executeUpdate();
            return n > 0 ? "Barang berhasil dihapus" : "Barang tidak ditemukan";
        } catch (SQLException e) {
            return "Gagal hapus barang: " + e.getMessage();
        }
    }

    public String createSantri(String nis, String nama, String kelas, boolean aktif) {
        String sql = "INSERT INTO santri(nis,nama,kelas,aktif,saldo) VALUES(?,?,?,?,0)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nis);
            ps.setString(2, nama);
            ps.setString(3, kelas);
            ps.setBoolean(4, aktif);
            ps.executeUpdate();
            return "Santri berhasil ditambahkan";
        } catch (SQLException e) {
            return "Gagal tambah santri: " + e.getMessage();
        }
    }

    public String createSupplier(String nama, String kontak, String alamat) {
        String sql = "INSERT INTO supplier(nama,kontak,alamat) VALUES(?,?,?)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nama);
            ps.setString(2, kontak);
            ps.setString(3, alamat);
            ps.executeUpdate();
            return "Supplier berhasil ditambahkan";
        } catch (SQLException e) {
            return "Gagal tambah supplier: " + e.getMessage();
        }
    }

    public String createUser(String username, String password, Role role) {
        String sql = "INSERT INTO users(username,password_hash,role) VALUES(?,SHA2(?,256),?)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role.name());
            ps.executeUpdate();
            return "User berhasil ditambahkan";
        } catch (SQLException e) {
            return "Gagal tambah user: " + e.getMessage();
        }
    }

    public String renderReceipt(String trxNo) {
        String headSql = "SELECT t.no_trx,t.tanggal,t.grand_total,t.bayar_tunai,t.bayar_deposit,u.username FROM transaksi t JOIN users u ON u.id=t.kasir_id WHERE t.no_trx=?";
        String detailSql = "SELECT b.nama,d.qty,d.harga_jual,d.diskon_item,d.ppn_persen,d.total FROM transaksi_detail d JOIN barang b ON b.id=d.barang_id JOIN transaksi t ON t.id=d.transaksi_id WHERE t.no_trx=?";
        StringBuilder sb = new StringBuilder();
        sb.append("KOPERASI PONDOK PESANTREN\n");
        sb.append("================================\n");
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(headSql)) {
                ps.setString(1, trxNo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return "Struk tidak ditemukan";
                    sb.append("No: ").append(rs.getString("no_trx")).append("\n");
                    sb.append("Tgl: ").append(rs.getTimestamp("tanggal")).append("\n");
                    sb.append("Kasir: ").append(rs.getString("username")).append("\n");
                    sb.append("--------------------------------\n");
                    try (PreparedStatement dps = conn.prepareStatement(detailSql)) {
                        dps.setString(1, trxNo);
                        try (ResultSet drs = dps.executeQuery()) {
                            while (drs.next()) {
                                sb.append(drs.getString(1)).append(" x").append(drs.getBigDecimal(2)).append("\n");
                                sb.append("  harga ").append(drs.getBigDecimal(3)).append(" disc ").append(drs.getBigDecimal(4)).append(" ppn ").append(drs.getBigDecimal(5)).append("%\n");
                                sb.append("  total ").append(drs.getBigDecimal(6)).append("\n");
                            }
                        }
                    }
                    sb.append("--------------------------------\n");
                    sb.append("Grand Total: ").append(rs.getBigDecimal("grand_total")).append("\n");
                    sb.append("Tunai: ").append(rs.getBigDecimal("bayar_tunai")).append("\n");
                    sb.append("Deposit: ").append(rs.getBigDecimal("bayar_deposit")).append("\n");
                }
            }
        } catch (SQLException e) {
            return "Gagal generate struk: " + e.getMessage();
        }
        sb.append("================================\n");
        sb.append("Terima kasih\n");
        return sb.toString();
    }

    public String printReceiptThermal(String trxNo) {
        try {
            JasperPrint print = buildReceiptPrint(trxNo);
            String printerName = AppConfig.get("app.printer.name");
            if (printerName == null || printerName.isBlank()) {
                boolean sent = net.sf.jasperreports.engine.JasperPrintManager.printReport(print, false);
                return sent ? "Perintah cetak dikirim ke printer default" : "Cetak dibatalkan";
            }
            PrintService selected = findPrinterByName(printerName.trim());
            if (selected == null) {
                return "Printer tidak ditemukan: " + printerName;
            }
            JRPrintServiceExporter exporter = new JRPrintServiceExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            SimplePrintServiceExporterConfiguration cfg = new SimplePrintServiceExporterConfiguration();
            cfg.setPrintService(selected);
            cfg.setDisplayPageDialog(false);
            cfg.setDisplayPrintDialog(false);
            exporter.setConfiguration(cfg);
            exporter.exportReport();
            return "Perintah cetak dikirim ke printer: " + selected.getName();
        } catch (Exception e) {
            return "Gagal cetak thermal: " + e.getMessage();
        }
    }

    public List<String> listPrinterNames() {
        List<String> names = new ArrayList<>();
        for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
            names.add(ps.getName());
        }
        return names;
    }

    public String savePrinterSetting(String printerName, String paperSize) {
        if (!"58".equals(paperSize) && !"80".equals(paperSize)) {
            return "Ukuran kertas harus 58 atau 80";
        }
        if (printerName != null && !printerName.isBlank() && findPrinterByName(printerName.trim()) == null) {
            return "Printer tidak ditemukan: " + printerName;
        }
        AppConfig.set("app.paper.size", paperSize);
        AppConfig.set("app.printer.name", printerName == null ? "" : printerName.trim());
        AppConfig.save();
        return "Pengaturan printer tersimpan";
    }

    public String exportReceiptPdf(String trxNo, String outputPath) {
        try {
            JasperPrint print = buildReceiptPrint(trxNo);
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(print));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(new File(outputPath)));
            exporter.exportReport();
            return "PDF struk tersimpan: " + outputPath;
        } catch (Exception e) {
            return "Gagal export PDF struk: " + e.getMessage();
        }
    }

    private JasperPrint buildReceiptPrint(String trxNo) throws JRException {
        String receiptText = renderReceipt(trxNo);
        if ("Struk tidak ditemukan".equals(receiptText)) {
            throw new JRException(receiptText);
        }
        String paperSize = AppConfig.get("app.paper.size");
        String template = "58".equals(paperSize) ? "/reports/receipt_58.jrxml" : "/reports/receipt_80.jrxml";
        try (InputStream in = KasirService.class.getResourceAsStream(template)) {
            if (in == null) {
                throw new JRException("Template struk tidak ditemukan: " + template);
            }
            JasperReport report = JasperCompileManager.compileReport(in);
            var params = new HashMap<String, Object>();
            params.put("RECEIPT_TEXT", receiptText);
            return JasperFillManager.fillReport(report, params, new JREmptyDataSource(1));
        } catch (Exception e) {
            if (e instanceof JRException jr) throw jr;
            throw new JRException(e);
        }
    }

    private PrintService findPrinterByName(String name) {
        for (PrintService ps : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (ps.getName().equalsIgnoreCase(name)) {
                return ps;
            }
        }
        return null;
    }

    private void lockAndValidateStock(Connection conn, long barangId, BigDecimal qty) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT stok FROM barang WHERE id=? FOR UPDATE")) {
            ps.setLong(1, barangId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Barang tidak ditemukan: " + barangId);
                BigDecimal stok = rs.getBigDecimal(1);
                if (stok.compareTo(qty) < 0) throw new SQLException("Stok tidak cukup untuk barang " + barangId);
            }
        }
    }

    private BigDecimal currentSaldo(Connection conn, long santriId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT saldo FROM santri WHERE id=? FOR UPDATE")) {
            ps.setLong(1, santriId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Santri tidak ditemukan");
                return rs.getBigDecimal(1);
            }
        }
    }

    private void updateSaldo(Connection conn, long santriId, BigDecimal saldoBaru) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE santri SET saldo=? WHERE id=?")) {
            ps.setBigDecimal(1, saldoBaru);
            ps.setLong(2, santriId);
            ps.executeUpdate();
        }
    }

    private void insertWallet(Connection conn, long santriId, String tipe, BigDecimal nominal, BigDecimal saldoAfter, String refNo,
                              String reason, Long authorizedBy, long createdBy) throws SQLException {
        String sql = "INSERT INTO wallet_transaction(santri_id,tipe,nominal,saldo_setelah,ref_no,reason,authorized_by,created_by,created_at) VALUES(?,?,?,?,?,?,?,?,NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, santriId);
            ps.setString(2, tipe);
            ps.setBigDecimal(3, nominal);
            ps.setBigDecimal(4, saldoAfter);
            if (refNo == null) ps.setNull(5, Types.VARCHAR); else ps.setString(5, refNo);
            if (reason == null) ps.setNull(6, Types.VARCHAR); else ps.setString(6, reason);
            if (authorizedBy == null) ps.setNull(7, Types.BIGINT); else ps.setLong(7, authorizedBy);
            ps.setLong(8, createdBy);
            ps.executeUpdate();
        }
    }

    private String nextTrxNo(Connection conn) throws SQLException {
        String prefix = "TRX-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        int next = 1;
        try (PreparedStatement ps = conn.prepareStatement("SELECT no_trx FROM transaksi WHERE no_trx LIKE ? ORDER BY id DESC LIMIT 1")) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String[] parts = rs.getString(1).split("-");
                    next = Integer.parseInt(parts[2]) + 1;
                }
            }
        }
        return prefix + String.format("%04d", next);
    }

    private void updateStock(Connection conn, long barangId, BigDecimal delta, String tipe, String kategori, String note, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE barang SET stok=stok+? WHERE id=?")) {
            ps.setBigDecimal(1, delta);
            ps.setLong(2, barangId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO stock_movement(barang_id,tipe,kategori,qty,note,created_by,created_at) VALUES(?,?,?,?,?,?,NOW())")) {
            ps.setLong(1, barangId);
            ps.setString(2, tipe);
            ps.setString(3, kategori);
            ps.setBigDecimal(4, delta);
            ps.setString(5, note);
            ps.setLong(6, userId);
            ps.executeUpdate();
        }
    }

    private BigDecimal readStock(Connection conn, long barangId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT stok FROM barang WHERE id=? FOR UPDATE")) {
            ps.setLong(1, barangId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Barang tidak ditemukan");
                return rs.getBigDecimal(1);
            }
        }
    }

    private void updateAvgCost(Connection conn, long barangId, BigDecimal qtyIn, BigDecimal priceIn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT stok, harga_beli_avg FROM barang WHERE id=? FOR UPDATE")) {
            ps.setLong(1, barangId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Barang tidak ditemukan");
                BigDecimal stok = rs.getBigDecimal(1);
                BigDecimal avg = rs.getBigDecimal(2);
                BigDecimal totalOld = stok.multiply(avg);
                BigDecimal totalNew = qtyIn.multiply(priceIn);
                BigDecimal stokBaru = stok.add(qtyIn);
                BigDecimal avgBaru = stokBaru.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : totalOld.add(totalNew).divide(stokBaru, 2, RoundingMode.HALF_UP);
                try (PreparedStatement up = conn.prepareStatement("UPDATE barang SET harga_beli_avg=? WHERE id=?")) {
                    up.setBigDecimal(1, avgBaru);
                    up.setLong(2, barangId);
                    up.executeUpdate();
                }
            }
        }
    }

    public record SaleItem(long barangId, BigDecimal qty, BigDecimal harga, BigDecimal diskon, BigDecimal ppnPersen) {}
    public record PurchaseItem(long barangId, BigDecimal qty, BigDecimal hargaBeli) {}
    public record StockOpnameItem(long barangId, BigDecimal stokFisik) {}
}
