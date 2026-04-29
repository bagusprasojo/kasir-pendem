package com.kasirpendem;

import com.kasirpendem.config.AppConfig;
import com.kasirpendem.model.Role;
import com.kasirpendem.model.User;
import com.kasirpendem.service.KasirService;
import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainApp extends Application {
    private final KasirService service = new KasirService();
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        primaryStage.setTitle("Kasir Pendem");
        primaryStage.setScene(new Scene(loginPane(), 460, 260));
        primaryStage.show();
    }

    private VBox loginPane() {
        Label title = new Label("Login Kasir Pendem");
        TextField user = new TextField();
        user.setPromptText("Username");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Password");
        Label msg = new Label();
        Button btn = new Button("Login");
        btn.setOnAction(e -> {
            var auth = service.login(user.getText().trim(), pass.getText());
            if (auth.isPresent()) {
                stage.setScene(new Scene(homePane(auth.get()), 760, 520));
            } else {
                msg.setText("Login gagal");
            }
        });
        VBox box = new VBox(10, title, user, pass, btn, msg);
        box.setPadding(new Insets(20));
        return box;
    }

    private VBox homePane(User user) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        Label heading = new Label("Dashboard Kasir Pendem");
        heading.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label userInfo = new Label("User: " + user.username() + " | Role: " + user.role());
        root.getChildren().addAll(heading, userInfo);

        FlowPane row = new FlowPane();
        row.setHgap(12);
        row.setVgap(12);

        VBox operasional = sectionCard("Operasional");
        if (user.role() == Role.ADMIN || user.role() == Role.KASIR) {
            operasional.getChildren().add(menuButton("POS", () -> openWindow("POS", posForm(user), 980, 720)));
        }
        if (user.role() == Role.ADMIN) {
            operasional.getChildren().add(menuButton("Deposit", () -> openWindow("Deposit", depositForm(user), 620, 440)));
            operasional.getChildren().add(menuButton("Inventori", () -> openWindow("Inventori", inventoryForm(user), 760, 520)));
            operasional.getChildren().add(menuButton("Kulakan", () -> openWindow("Kulakan", kulakanForm(user), 720, 560)));
            operasional.getChildren().add(menuButton("Stock Opname", () -> openWindow("Stock Opname", stockOpnameForm(user), 720, 560)));
        }
        if (operasional.getChildren().size() > 1) row.getChildren().add(operasional);

        if (user.role() == Role.ADMIN) {
            VBox master = sectionCard("Master Data");
            master.getChildren().add(menuButton("Master Barang", () -> openWindow("Master Barang", masterBarangForm(), 760, 420)));
            master.getChildren().add(menuButton("Master Santri", () -> openWindow("Master Santri", masterSantriForm(), 760, 380)));
            master.getChildren().add(menuButton("Master Supplier", () -> openWindow("Master Supplier", masterSupplierForm(), 760, 380)));
            master.getChildren().add(menuButton("Master User", () -> openWindow("Master User", masterUserForm(), 760, 360)));
            row.getChildren().add(master);
        }

        if (user.role() == Role.ADMIN || user.role() == Role.MANAGER) {
            VBox laporan = sectionCard("Laporan");
            laporan.getChildren().add(menuButton("Laporan Penjualan", () -> openWindow("Laporan Penjualan", reportPenjualanForm(), 920, 620)));
            laporan.getChildren().add(menuButton("Laporan Deposit", () -> openWindow("Laporan Deposit", reportDepositForm(), 920, 620)));
            laporan.getChildren().add(menuButton("Laporan Stok Minimum", () -> openWindow("Laporan Stok Minimum", reportStokForm(), 920, 620)));
            laporan.getChildren().add(menuButton("Pengaturan Printer", () -> openWindow("Pengaturan Printer", printerSettingForm(), 640, 280)));
            row.getChildren().add(laporan);
        }

        root.getChildren().add(row);
        return root;
    }

    private Button menuButton(String text, Runnable action) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: #1f6aa5; -fx-text-fill: white; -fx-font-weight: bold;");
        b.setOnAction(e -> action.run());
        return b;
    }

    private void openWindow(String title, VBox content, int w, int h) {
        Stage s = new Stage();
        s.initOwner(stage);
        s.initModality(Modality.NONE);
        s.setTitle(title);
        s.setScene(new Scene(content, w, h));
        s.sizeToScene();
        s.setMinWidth(Math.min(w, 560));
        s.setMinHeight(Math.min(h, 340));
        s.show();
    }

    private VBox sectionCard(String title) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        VBox card = new VBox(8, lbl);
        card.setPadding(new Insets(12));
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: #f5f7fa; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");
        return card;
    }

    private VBox posForm(User user) {
        List<BarangRow> allBarang = loadBarangRows();
        Map<Long, BarangRow> barangById = new HashMap<>();
        allBarang.forEach(b -> barangById.put(b.id(), b));
        ObservableList<CartItem> cart = FXCollections.observableArrayList();

        TextField scanField = new TextField();
        scanField.setPromptText("Scan barcode/kode/nama barang");
        TextField addQty = new TextField("1");
        addQty.setPromptText("Qty");
        ListView<String> barangList = new ListView<>();
        barangList.setPrefHeight(260);
        barangList.getItems().setAll(allBarang.stream().map(BarangRow::display).toList());

        Button cariBarang = new Button("Cari");
        Button tambahBarang = new Button("Tambah ke Keranjang");
        CheckBox autoAdd = new CheckBox("Auto-add saat match tunggal");
        autoAdd.setSelected(true);

        TableView<CartItem> cartTable = new TableView<>(cart);
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<CartItem, String> colNama = new TableColumn<>("Barang");
        colNama.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().nama));
        TableColumn<CartItem, BigDecimal> colQty = new TableColumn<>("Qty");
        colQty.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().qty));
        TableColumn<CartItem, BigDecimal> colHarga = new TableColumn<>("Harga");
        colHarga.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().harga));
        TableColumn<CartItem, BigDecimal> colDisc = new TableColumn<>("Diskon");
        colDisc.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().diskon));
        TableColumn<CartItem, BigDecimal> colPpn = new TableColumn<>("PPN%");
        colPpn.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().ppn));
        TableColumn<CartItem, BigDecimal> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().subtotal()));
        cartTable.getColumns().addAll(colNama, colQty, colHarga, colDisc, colPpn, colSub);
        cartTable.setPrefHeight(320);

        Button plusQty = new Button("+ Qty");
        Button minusQty = new Button("- Qty");
        Button hapusItem = new Button("Hapus Item");

        TextField santriId = new TextField();
        santriId.setPromptText("Santri ID (opsional)");
        TextField santriKeyword = new TextField();
        santriKeyword.setPromptText("Cari santri (NIS/Nama)");
        ListView<String> santriResult = new ListView<>();
        santriResult.setPrefHeight(120);
        TextField tunai = new TextField("0");
        tunai.setPromptText("Tunai");
        TextField deposit = new TextField("0");
        deposit.setPromptText("Deposit");
        TextField trxNo = new TextField();
        trxNo.setPromptText("No transaksi untuk cetak ulang struk");
        Label lblSubtotal = new Label("Subtotal: 0");
        Label lblDiskon = new Label("Diskon: 0");
        Label lblPpn = new Label("PPN: 0");
        Label lblGrand = new Label("Grand Total: 0");
        Label lblKembalian = new Label("Kembalian: 0");
        Label msg = new Label();

        Button cariSantri = new Button("Cari Santri");
        Button pay = new Button("Proses Transaksi");
        Button print = new Button("Cetak Struk Teks");
        Button printThermal = new Button("Print Thermal");
        Button exportReceiptPdf = new Button("Export Struk PDF");

        Runnable recalc = () -> {
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal totalDiskon = BigDecimal.ZERO;
            BigDecimal totalPpn = BigDecimal.ZERO;
            for (CartItem c : cart) {
                BigDecimal line = c.harga.multiply(c.qty);
                subtotal = subtotal.add(line);
                totalDiskon = totalDiskon.add(c.diskon);
                BigDecimal ppnNom = line.subtract(c.diskon).multiply(c.ppn).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                totalPpn = totalPpn.add(ppnNom);
            }
            BigDecimal grand = subtotal.subtract(totalDiskon).add(totalPpn);
            BigDecimal tunaiVal = safeMoney(tunai.getText());
            BigDecimal depVal = safeMoney(deposit.getText());
            BigDecimal kembali = tunaiVal.add(depVal).subtract(grand);
            lblSubtotal.setText("Subtotal: " + subtotal);
            lblDiskon.setText("Diskon: " + totalDiskon);
            lblPpn.setText("PPN: " + totalPpn);
            lblGrand.setText("Grand Total: " + grand);
            lblKembalian.setText("Kembalian: " + kembali);
        };

        Runnable filterBarang = () -> {
            String k = scanField.getText() == null ? "" : scanField.getText().trim().toLowerCase();
            List<String> filtered = allBarang.stream()
                    .filter(b -> k.isBlank()
                            || b.kode().toLowerCase().contains(k)
                            || b.barcode().toLowerCase().contains(k)
                            || b.nama().toLowerCase().contains(k))
                    .sorted(Comparator.comparing(BarangRow::nama))
                    .map(BarangRow::display)
                    .collect(Collectors.toList());
            barangList.getItems().setAll(filtered);
            if (autoAdd.isSelected() && !k.isBlank() && filtered.size() == 1) {
                barangList.getSelectionModel().select(0);
                tambahBarang.fire();
            }
        };

        Runnable addSelectedBarang = () -> {
            String selected = barangList.getSelectionModel().getSelectedItem();
            if (selected == null || selected.isBlank()) {
                msg.setText("Pilih barang terlebih dahulu");
                return;
            }
            long barangId = Long.parseLong(selected.split("\\|")[0].trim());
            BarangRow b = barangById.get(barangId);
            if (b == null) {
                msg.setText("Barang tidak ditemukan");
                return;
            }
            BigDecimal qty = safeMoney(addQty.getText());
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                msg.setText("Qty harus > 0");
                return;
            }
            CartItem exist = cart.stream().filter(c -> c.barangId == barangId).findFirst().orElse(null);
            if (exist != null) {
                exist.qty = exist.qty.add(qty);
                cartTable.refresh();
            } else {
                cart.add(new CartItem(barangId, b.nama(), qty, b.harga(), BigDecimal.ZERO, b.ppn()));
            }
            recalc.run();
            msg.setText("Barang ditambahkan ke keranjang");
            scanField.clear();
            addQty.setText("1");
            scanField.requestFocus();
        };

        scanField.setOnAction(e -> {
            filterBarang.run();
            if (!barangList.getItems().isEmpty()) {
                barangList.getSelectionModel().select(0);
                addSelectedBarang.run();
            }
        });
        cariBarang.setOnAction(e -> filterBarang.run());
        tambahBarang.setOnAction(e -> addSelectedBarang.run());

        plusQty.setOnAction(e -> {
            CartItem selected = cartTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            selected.qty = selected.qty.add(BigDecimal.ONE);
            cartTable.refresh();
            recalc.run();
        });
        minusQty.setOnAction(e -> {
            CartItem selected = cartTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            if (selected.qty.compareTo(BigDecimal.ONE) <= 0) return;
            selected.qty = selected.qty.subtract(BigDecimal.ONE);
            cartTable.refresh();
            recalc.run();
        });
        hapusItem.setOnAction(e -> {
            CartItem selected = cartTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            cart.remove(selected);
            recalc.run();
        });

        cariSantri.setOnAction(e -> santriResult.getItems().setAll(service.searchSantri(santriKeyword.getText())));
        santriResult.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null || newV.isBlank()) return;
            String[] parts = newV.split("\\|");
            if (parts.length > 0) santriId.setText(parts[0].trim());
        });
        tunai.textProperty().addListener((a, b, c) -> recalc.run());
        deposit.textProperty().addListener((a, b, c) -> recalc.run());

        pay.setOnAction(e -> {
            try {
                if (cart.isEmpty()) {
                    msg.setText("Item transaksi tidak boleh kosong");
                    return;
                }
                List<KasirService.SaleItem> saleItems = cart.stream()
                        .map(c -> new KasirService.SaleItem(c.barangId, c.qty, c.harga, c.diskon, c.ppn))
                        .toList();
                Long sid = santriId.getText().isBlank() ? null : parseLong(santriId.getText(), "Santri ID");
                String res = service.processSale(user.id(), sid, saleItems, parseMoney(tunai.getText(), "Tunai"), parseMoney(deposit.getText(), "Deposit"));
                msg.setText(res);
                if (res.startsWith("Transaksi sukses:")) {
                    String no = res.replace("Transaksi sukses:", "").trim();
                    trxNo.setText(no);
                    cart.clear();
                    recalc.run();
                }
            } catch (Exception ex) {
                msg.setText("Input tidak valid: " + ex.getMessage());
            }
        });
        print.setOnAction(e -> msg.setText("Struk teks siap ditampilkan lewat cetak/export untuk trx " + trxNo.getText().trim()));
        printThermal.setOnAction(e -> msg.setText(service.printReceiptThermal(trxNo.getText().trim())));
        exportReceiptPdf.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            File out = chooser.showSaveDialog(stage);
            if (out != null) msg.setText(service.exportReceiptPdf(trxNo.getText().trim(), out.getAbsolutePath()));
        });

        VBox scanBox = new VBox(8, new Label("Cari/Scan Barang"), new HBox(8, scanField, addQty), new HBox(8, cariBarang, tambahBarang), barangList, autoAdd);
        VBox center = new VBox(8, new Label("Keranjang"), cartTable, new HBox(8, plusQty, minusQty, hapusItem), new Separator(), scanBox);
        VBox right = new VBox(8,
                new Label("Santri (opsional untuk deposit)"),
                new HBox(8, santriId, santriKeyword, cariSantri),
                santriResult,
                new Separator(),
                new Label("Pembayaran"),
                new HBox(8, tunai, deposit),
                lblSubtotal, lblDiskon, lblPpn, lblGrand, lblKembalian,
                new HBox(8, pay),
                new Separator(),
                new HBox(8, trxNo, print, printThermal, exportReceiptPdf),
                msg
        );
        center.setPrefWidth(720);
        right.setPrefWidth(360);
        BorderPane pane = new BorderPane();
        pane.setCenter(center);
        pane.setRight(right);
        pane.setPadding(new Insets(10));
        VBox root = new VBox(8, new Label("POS"), pane);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-background-color: #eef3f8;");
        root.setFocusTraversable(true);
        root.setOnMouseClicked(e -> root.requestFocus());
        center.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 10;");
        right.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 10;");
        pay.setStyle("-fx-background-color: #1f6aa5; -fx-text-fill: white; -fx-font-weight: bold;");
        tambahBarang.setStyle("-fx-background-color: #2f855a; -fx-text-fill: white; -fx-font-weight: bold;");

        root.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F2) {
                scanField.requestFocus();
                e.consume();
            } else if (e.getCode() == KeyCode.F4) {
                pay.fire();
                e.consume();
            } else if (e.getCode() == KeyCode.DELETE) {
                CartItem selected = cartTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    cart.remove(selected);
                    recalc.run();
                }
                e.consume();
            }
        });
        recalc.run();
        scanField.requestFocus();
        return root;
    }

    private VBox depositForm(User user) {
        TextField santriId = new TextField();
        TextField amount = new TextField();
        TextField walletId = new TextField();
        TextField reason = new TextField();
        Label msg = new Label();
        santriId.setPromptText("Santri ID");
        amount.setPromptText("Nominal topup");
        walletId.setPromptText("WalletTx ID");
        reason.setPromptText("Alasan reversal");

        Button topup = new Button("Topup");
        topup.setOnAction(e -> msg.setText(service.processTopup(parseLong(santriId.getText(), "Santri ID"), parseMoney(amount.getText(), "Nominal"), user.id())));
        Button reverse = new Button("Batalkan Topup");
        reverse.setOnAction(e -> msg.setText(service.reverseTopup(parseLong(walletId.getText(), "WalletTx ID"), reason.getText().trim(), user.id())));

        VBox box = new VBox(8, new Label("Deposit Santri"), santriId, amount, topup, new Separator(), walletId, reason, reverse, msg);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox inventoryForm(User user) {
        TextArea log = new TextArea();
        TextField barangId = new TextField();
        TextField qty = new TextField();
        ComboBox<String> kategori = new ComboBox<>();
        kategori.getItems().addAll("RUSAK", "HILANG", "PEMAKAIAN_INTERNAL");
        kategori.getSelectionModel().selectFirst();
        barangId.setPromptText("Barang ID");
        qty.setPromptText("Qty keluar");
        Button out = new Button("Catat Stok Keluar");
        out.setOnAction(e -> log.setText(service.stockOutNonSales(parseLong(barangId.getText(), "Barang ID"), parseMoney(qty.getText(), "Qty"), kategori.getValue(), "Stok keluar non-penjualan", user.id())));
        Button refresh = new Button("Refresh Barang");
        refresh.setOnAction(e -> log.setText(String.join("\n", service.listBarang())));
        VBox box = new VBox(8, new Label("Inventori"), log, new HBox(8, barangId, qty, kategori, out, refresh));
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox kulakanForm(User user) {
        TextField supplierId = new TextField();
        TextArea items = new TextArea();
        Label msg = new Label();
        supplierId.setPromptText("Supplier ID");
        items.setPromptText("Format: barangId,qty,hargaBeli");
        Button save = new Button("Simpan Kulakan");
        save.setOnAction(e -> {
            try {
                List<KasirService.PurchaseItem> list = new ArrayList<>();
                for (String line : items.getText().split("\\R")) {
                    if (line.isBlank()) continue;
                    String[] c = line.split(",");
                    list.add(new KasirService.PurchaseItem(parseLong(c[0], "Barang ID"), parseMoney(c[1], "Qty"), parseMoney(c[2], "Harga beli")));
                }
                msg.setText(service.inputPembelian(parseLong(supplierId.getText(), "Supplier ID"), list, user.id()));
            } catch (Exception ex) {
                msg.setText("Input tidak valid: " + ex.getMessage());
            }
        });
        VBox box = new VBox(8, new Label("Kulakan"), supplierId, items, save, msg);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox stockOpnameForm(User user) {
        TextArea data = new TextArea();
        data.setPromptText("Format seluruh barang: barangId,stokFisik");
        Label msg = new Label();
        Button proses = new Button("Proses Stock Opname");
        proses.setOnAction(e -> {
            try {
                List<KasirService.StockOpnameItem> list = new ArrayList<>();
                for (String line : data.getText().split("\\R")) {
                    if (line.isBlank()) continue;
                    String[] c = line.split(",");
                    list.add(new KasirService.StockOpnameItem(parseLong(c[0], "Barang ID"), parseMoney(c[1], "Stok fisik")));
                }
                msg.setText(service.stockOpname(list, user.id()));
            } catch (Exception ex) {
                msg.setText("Input tidak valid: " + ex.getMessage());
            }
        });
        VBox box = new VBox(8, new Label("Stock Opname (wajib seluruh barang)"), data, proses, msg);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox masterBarangForm() {
        Label msg = new Label();
        TextField search = new TextField();
        search.setPromptText("Cari kode/barcode/nama barang");

        ObservableList<BarangMasterRow> rows = FXCollections.observableArrayList();
        TableView<BarangMasterRow> table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(300);

        TableColumn<BarangMasterRow, Long> cId = new TableColumn<>("ID");
        cId.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().id()));
        TableColumn<BarangMasterRow, String> cKode = new TableColumn<>("Kode");
        cKode.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().kode()));
        TableColumn<BarangMasterRow, String> cBarcode = new TableColumn<>("Barcode");
        cBarcode.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().barcode()));
        TableColumn<BarangMasterRow, String> cNama = new TableColumn<>("Nama");
        cNama.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().nama()));
        TableColumn<BarangMasterRow, String> cSatuan = new TableColumn<>("Satuan");
        cSatuan.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().satuan()));
        TableColumn<BarangMasterRow, BigDecimal> cHarga = new TableColumn<>("Harga");
        cHarga.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().harga()));
        TableColumn<BarangMasterRow, BigDecimal> cPpn = new TableColumn<>("PPN");
        cPpn.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().ppn()));
        TableColumn<BarangMasterRow, BigDecimal> cStok = new TableColumn<>("Stok");
        cStok.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().stok()));
        TableColumn<BarangMasterRow, BigDecimal> cStokMin = new TableColumn<>("Stok Min");
        cStokMin.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().stokMin()));
        table.getColumns().addAll(cId, cKode, cBarcode, cNama, cSatuan, cHarga, cPpn, cStok, cStokMin);

        TextField id = new TextField();
        id.setDisable(true);
        TextField kode = new TextField();
        TextField barcode = new TextField();
        TextField nama = new TextField();
        TextField satuan = new TextField();
        TextField harga = new TextField();
        TextField ppn = new TextField("0");
        TextField stokMin = new TextField("0");

        Runnable clearForm = () -> {
            id.clear();
            kode.clear();
            barcode.clear();
            nama.clear();
            satuan.clear();
            harga.clear();
            ppn.setText("0");
            stokMin.setText("0");
            msg.setText("");
            kode.requestFocus();
        };

        Runnable loadRows = () -> {
            rows.setAll(service.listBarangMaster(search.getText()).stream().map(this::parseBarangMasterRow).toList());
            table.refresh();
        };

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, v) -> {
            if (v == null) return;
            id.setText(String.valueOf(v.id()));
            kode.setText(v.kode());
            barcode.setText(v.barcode());
            nama.setText(v.nama());
            satuan.setText(v.satuan());
            harga.setText(v.harga().toPlainString());
            ppn.setText(v.ppn().toPlainString());
            stokMin.setText(v.stokMin().toPlainString());
        });

        search.textProperty().addListener((obs, oldV, v) -> loadRows.run());

        Button baru = new Button("Baru (Ctrl+N)");
        baru.setOnAction(e -> clearForm.run());
        Button simpan = new Button("Simpan (Ctrl+S)");
        simpan.setOnAction(e -> {
            try {
                validateBarangForm(kode, nama, satuan, harga, ppn, stokMin);
                String res = service.createBarang(kode.getText().trim(), barcode.getText().trim(), nama.getText().trim(), satuan.getText().trim(),
                        parseMoney(harga.getText(), "Harga"), parseMoney(ppn.getText(), "PPN"), parseMoney(stokMin.getText(), "Stok Min"));
                msg.setText(res);
                loadRows.run();
                if (res.startsWith("Barang berhasil")) clearForm.run();
            } catch (Exception ex) {
                msg.setText("Validasi gagal: " + ex.getMessage());
            }
        });
        Button update = new Button("Update");
        update.setOnAction(e -> {
            try {
                if (id.getText().isBlank()) {
                    msg.setText("Pilih barang dari tabel untuk update");
                    return;
                }
                validateBarangForm(kode, nama, satuan, harga, ppn, stokMin);
                String res = service.updateBarang(parseLong(id.getText(), "ID"), kode.getText().trim(), barcode.getText().trim(), nama.getText().trim(), satuan.getText().trim(),
                        parseMoney(harga.getText(), "Harga"), parseMoney(ppn.getText(), "PPN"), parseMoney(stokMin.getText(), "Stok Min"));
                msg.setText(res);
                loadRows.run();
            } catch (Exception ex) {
                msg.setText("Validasi gagal: " + ex.getMessage());
            }
        });
        Button hapus = new Button("Hapus (Del)");
        hapus.setOnAction(e -> {
            if (id.getText().isBlank()) {
                msg.setText("Pilih barang dari tabel untuk hapus");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Hapus barang ID " + id.getText() + "?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Konfirmasi Hapus");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    String res = service.deleteBarang(parseLong(id.getText(), "ID"));
                    msg.setText(res);
                    loadRows.run();
                    if (res.startsWith("Barang berhasil")) clearForm.run();
                }
            });
        });
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> loadRows.run());

        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8);
        g.addRow(0, new Label("ID"), id);
        g.addRow(1, new Label("Kode"), kode);
        g.addRow(2, new Label("Barcode"), barcode);
        g.addRow(3, new Label("Nama"), nama);
        g.addRow(4, new Label("Satuan"), satuan);
        g.addRow(5, new Label("Harga Jual"), harga);
        g.addRow(6, new Label("PPN %"), ppn);
        g.addRow(7, new Label("Stok Minimum"), stokMin);

        VBox box = new VBox(10,
                new Label("Master Barang"),
                new HBox(8, search, refresh),
                table,
                g,
                new HBox(8, baru, simpan, update, hapus),
                msg
        );
        box.setPadding(new Insets(10));
        box.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.N) {
                baru.fire();
            } else if (e.isControlDown() && e.getCode() == KeyCode.S) {
                if (id.getText().isBlank()) simpan.fire(); else update.fire();
            } else if (e.getCode() == KeyCode.DELETE) {
                hapus.fire();
            }
        });
        box.setFocusTraversable(true);
        loadRows.run();
        return box;
    }

    private VBox masterSantriForm() {
        Label msg = new Label();
        TextField nis = new TextField();
        TextField nama = new TextField();
        TextField kelas = new TextField();
        CheckBox aktif = new CheckBox("Aktif");
        aktif.setSelected(true);
        Button save = new Button("Tambah Santri");
        save.setOnAction(e -> msg.setText(service.createSantri(nis.getText().trim(), nama.getText().trim(), kelas.getText().trim(), aktif.isSelected())));
        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8);
        g.addRow(0, new Label("NIS"), nis);
        g.addRow(1, new Label("Nama"), nama);
        g.addRow(2, new Label("Kelas"), kelas);
        g.addRow(3, new Label("Status"), aktif);
        VBox box = new VBox(10, new Label("Master Santri"), g, save, msg);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox masterSupplierForm() {
        Label msg = new Label();
        TextField nama = new TextField();
        TextField kontak = new TextField();
        TextField alamat = new TextField();
        Button save = new Button("Tambah Supplier");
        save.setOnAction(e -> msg.setText(service.createSupplier(nama.getText().trim(), kontak.getText().trim(), alamat.getText().trim())));
        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8);
        g.addRow(0, new Label("Nama"), nama);
        g.addRow(1, new Label("Kontak"), kontak);
        g.addRow(2, new Label("Alamat"), alamat);
        VBox box = new VBox(10, new Label("Master Supplier"), g, save, msg);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox masterUserForm() {
        Label msg = new Label();
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<Role> role = new ComboBox<>();
        role.getItems().addAll(Role.ADMIN, Role.KASIR, Role.MANAGER);
        role.getSelectionModel().select(Role.KASIR);
        Button save = new Button("Tambah User");
        save.setOnAction(e -> msg.setText(service.createUser(username.getText().trim(), password.getText(), role.getValue())));
        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8);
        g.addRow(0, new Label("Username"), username);
        g.addRow(1, new Label("Password"), password);
        g.addRow(2, new Label("Role"), role);
        VBox box = new VBox(10, new Label("Master User"), g, save, msg);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox reportPenjualanForm() {
        return reportForm("Laporan Penjualan", (f, t) -> service.reportPenjualan(f, t));
    }

    private VBox reportDepositForm() {
        return reportForm("Laporan Deposit", (f, t) -> service.reportDeposit(f, t));
    }

    private VBox reportStokForm() {
        DatePicker from = new DatePicker(LocalDate.now().minusDays(7));
        DatePicker to = new DatePicker(LocalDate.now());
        TextArea out = new TextArea();
        Button load = new Button("Load");
        load.setOnAction(e -> out.setText(String.join("\n", service.reportStokMinimum())));
        Button export = new Button("Export CSV (;)");
        export.setOnAction(e -> exportCsv(out.getText()));
        VBox box = new VBox(8, new Label("Laporan Stok Minimum"), new HBox(8, new Label("Dari"), from, new Label("Sampai"), to), new HBox(8, load, export), out);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox reportForm(String title, ReportLoader loader) {
        DatePicker from = new DatePicker(LocalDate.now().minusDays(7));
        DatePicker to = new DatePicker(LocalDate.now());
        TextArea out = new TextArea();
        Button load = new Button("Load");
        load.setOnAction(e -> out.setText(String.join("\n", loader.load(from.getValue(), to.getValue()))));
        Button export = new Button("Export CSV (;)");
        export.setOnAction(e -> exportCsv(out.getText()));
        VBox box = new VBox(8, new Label(title), new HBox(8, new Label("Dari"), from, new Label("Sampai"), to), new HBox(8, load, export), out);
        box.setPadding(new Insets(10));
        return box;
    }

    private VBox printerSettingForm() {
        ComboBox<String> paper = new ComboBox<>();
        paper.getItems().addAll("58", "80");
        paper.getSelectionModel().select(AppConfig.get("app.paper.size"));
        ComboBox<String> printers = new ComboBox<>();
        printers.getItems().addAll(service.listPrinterNames());
        String current = AppConfig.get("app.printer.name");
        if (current != null && !current.isBlank()) printers.getSelectionModel().select(current);
        Label msg = new Label();
        Button refresh = new Button("Refresh Printer");
        refresh.setOnAction(e -> printers.getItems().setAll(service.listPrinterNames()));
        Button save = new Button("Simpan Pengaturan");
        save.setOnAction(e -> msg.setText(service.savePrinterSetting(printers.getValue(), paper.getValue())));
        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8);
        g.addRow(0, new Label("Paper size"), paper);
        g.addRow(1, new Label("Printer"), printers);
        VBox box = new VBox(10, new Label("Pengaturan Printer Thermal"), g, new HBox(8, refresh, save), msg);
        box.setPadding(new Insets(10));
        return box;
    }

    private void exportCsv(String content) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = fc.showSaveDialog(stage);
        if (file == null) return;
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Gagal export CSV: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private List<KasirService.SaleItem> parseSaleItems(String raw) {
        List<KasirService.SaleItem> saleItems = new ArrayList<>();
        for (String line : raw.split("\\R")) {
            if (line.isBlank() || !line.contains(",")) continue;
            String[] c = line.split(",");
            if (c.length != 5) throw new IllegalArgumentException("Format item POS salah: " + line);
            saleItems.add(new KasirService.SaleItem(parseLong(c[0], "Barang ID"), parseMoney(c[1], "Qty"), parseMoney(c[2], "Harga"), parseMoney(c[3], "Diskon"), parseMoney(c[4], "PPN")));
        }
        return saleItems;
    }

    private List<BarangRow> loadBarangRows() {
        List<BarangRow> rows = new ArrayList<>();
        for (String s : service.listBarangPos()) {
            String[] p = s.split("\\|");
            if (p.length < 7) continue;
            long id = Long.parseLong(p[0].trim());
            String kode = p[1].trim();
            String barcode = p[2].trim();
            String nama = p[3].trim();
            BigDecimal stok = safeMoney(p[4].trim());
            BigDecimal harga = safeMoney(p[5].trim());
            BigDecimal ppn = safeMoney(p[6].trim());
            rows.add(new BarangRow(id, kode, barcode, nama, stok, harga, ppn));
        }
        return rows;
    }

    private BigDecimal safeMoney(String value) {
        try {
            return new BigDecimal(value == null || value.isBlank() ? "0" : value.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private long parseLong(String value, String field) {
        String v = value == null ? "" : value.trim();
        if (v.isBlank()) throw new IllegalArgumentException(field + " wajib diisi");
        return Long.parseLong(v);
    }

    private BigDecimal parseMoney(String value, String field) {
        String v = value == null ? "" : value.trim();
        if (v.isBlank()) throw new IllegalArgumentException(field + " wajib diisi");
        return new BigDecimal(v);
    }

    private BarangMasterRow parseBarangMasterRow(String line) {
        String[] p = line.split("\\|");
        if (p.length < 9) throw new IllegalArgumentException("Format barang master tidak valid");
        return new BarangMasterRow(
                Long.parseLong(p[0].trim()),
                p[1].trim(),
                p[2].trim(),
                p[3].trim(),
                p[4].trim(),
                safeMoney(p[5].trim()),
                safeMoney(p[6].trim()),
                safeMoney(p[7].trim()),
                safeMoney(p[8].trim())
        );
    }

    private void validateBarangForm(TextField kode, TextField nama, TextField satuan, TextField harga, TextField ppn, TextField stokMin) {
        if (kode.getText().isBlank()) throw new IllegalArgumentException("Kode wajib diisi");
        if (nama.getText().isBlank()) throw new IllegalArgumentException("Nama wajib diisi");
        if (satuan.getText().isBlank()) throw new IllegalArgumentException("Satuan wajib diisi");
        BigDecimal h = parseMoney(harga.getText(), "Harga");
        BigDecimal p = parseMoney(ppn.getText(), "PPN");
        BigDecimal s = parseMoney(stokMin.getText(), "Stok Min");
        if (h.compareTo(BigDecimal.ZERO) < 0 || p.compareTo(BigDecimal.ZERO) < 0 || s.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Harga/PPN/Stok Min tidak boleh negatif");
        }
    }

    private record BarangRow(long id, String kode, String barcode, String nama, BigDecimal stok, BigDecimal harga, BigDecimal ppn) {
        String display() {
            return id + " | " + kode + " | " + nama + " | stok=" + stok + " | harga=" + harga;
        }
    }

    private static class CartItem {
        private final long barangId;
        private final String nama;
        private BigDecimal qty;
        private final BigDecimal harga;
        private final BigDecimal diskon;
        private final BigDecimal ppn;

        private CartItem(long barangId, String nama, BigDecimal qty, BigDecimal harga, BigDecimal diskon, BigDecimal ppn) {
            this.barangId = barangId;
            this.nama = nama;
            this.qty = qty;
            this.harga = harga;
            this.diskon = diskon;
            this.ppn = ppn;
        }

        private BigDecimal subtotal() {
            return harga.multiply(qty).subtract(diskon);
        }
    }

    private record BarangMasterRow(
            long id, String kode, String barcode, String nama, String satuan,
            BigDecimal harga, BigDecimal ppn, BigDecimal stok, BigDecimal stokMin
    ) {}

    public static void main(String[] args) {
        launch(args);
    }

    @FunctionalInterface
    private interface ReportLoader {
        List<String> load(LocalDate from, LocalDate to);
    }
}
