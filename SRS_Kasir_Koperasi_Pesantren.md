
# Software Requirements Specification (SRS)
## Aplikasi Kasir Koperasi Pondok Pesantren


## 1. Pendahuluan

### 1.1 Tujuan
Dokumen ini mendefinisikan kebutuhan sistem aplikasi kasir koperasi pesantren yang mencakup POS, deposit santri, inventori, dan laporan.

### 1.2 Ruang Lingkup
Aplikasi desktop berbasis JavaFX yang berjalan offline dengan fitur:
- Kasir (POS)
- Deposit santri
- Inventori
- Kulakan
- Stock opname
- Laporan

---

## 2. Gambaran Umum

### 2.1 Pengguna
- Admin (bisa akses semua menu)
- Kasir (hanya akses menu POS)
- Manager (Hanya akses menu laporan)

### 2.2 Lingkungan Sistem
- Desktop Windows
- Database MySQL lokal
- Printer thermal

---

## 3. Kebutuhan Fungsional

### 3.1 POS
- Input barang (via barcode scanner, kode manual)
- Diskon diberlakukan per item barang
- Pajak/PPN (Ada setting per barang)
- Keranjang belanja
- Pembayaran:
  - Tunai
  - Deposit
  - Kombinasi,  (operator bebas input nominal)
- Cetak struk

### 3.2 Deposit Santri
- Top-up saldo (Saldo tidak boleh minus, top-up bisa dibatalkan, ada batas minimal/maksimal top-up. Pembatasan minimal dan maksimal ada konfigurasinya)
- Pembatalan top-up boleh kapan saja,, Perlu alasan pembatalan , admin yang mengotorisasi
- Pembayaran menggunakan saldo
- Riwayat transaksi (ledger)

### 3.3 Inventori
- Manajemen stok (Valuasi stock average)
- Stok masuk & keluar
- Stok minimum
- Stok keluar non-penjualan perlu dicatat kategori (rusak/hilang/pemakaian internal)

### 3.4 Kulakan
- Input pembelian (Harga beli historis per supplier perlu disimpan, semua pembelian lunas)
- Update stok

### 3.5 Stock Opname
- Input stok fisik (tidak boleh parsial per rak/kategori)
- Penyesuaian

### 3.6 Laporan
- Penjualan
- Deposit
- Stok
- Periode laporan custom
- Ekspor laporan ke csv
- Printer thermal : ada config ukuran kertas

---

## 4. Kebutuhan Non-Fungsional

- Offline support
- Transaksi ACID
- Keamanan login cukup user dan password
- Backup data secara manual
- UI sederhana

---

## 5. Arsitektur

- Java 21 LTS + JDBC
- JavaFX 21 (UI)
- MySQL (wajib)
- JasperReports

---

## 6. Model Data

Entitas:
- user
- santri (NIS, Nama, Kelas, status aktif)
- barang
- transaksi
- transaksi_detail
- wallet_transaction
- supplier
- pembelian

---

## 7. Mekanisme Transaksi

START TRANSACTION:
- Simpan transaksi
- Kurangi stok 
- Kurangi saldo (jika deposit)
COMMIT

---

## 8. Lain-lain
- Perlu format tertentu untuk nomor struk/invoice yaitu : TRX-YYYYMMDD-XXXX
- Satu barang hanya bisa 1 barcode/satuan
- Notifikasi stok minimum tampil di laporan saja
- Default ukuran kertas 80mm, dan user bisa ubah di menu pengaturan
- Pemisah CSV pakai titik koma (;)

---

## 9. Penutup

Sistem dirancang untuk stabil, offline, dan mudah digunakan dengan fokus pada akurasi transaksi dan saldo.
