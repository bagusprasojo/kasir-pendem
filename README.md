# Kasir Pendem

Aplikasi desktop kasir koperasi pesantren berbasis JavaFX 21 + Java 21 + MySQL.

## Fitur
- Login role-based: `ADMIN`, `KASIR`, `MANAGER`
- POS: barcode/kode manual, diskon item, PPN per barang, pembayaran tunai/deposit/kombinasi
- POS: cetak struk ulang berdasarkan nomor transaksi
- POS: print thermal nyata via JasperReports ke default printer
- Deposit santri: topup, reversal topup (otorisasi admin), ledger
- Inventori: stok masuk/keluar, stok minimum (notifikasi via laporan)
- Kulakan: pembelian supplier + update stok + average cost
- Stock opname penuh
- Master data: tambah barang, santri, supplier, user
- Laporan penjualan/deposit/stok minimum + export CSV (`;`)

## Setup
1. Buat database dan tabel:
   - Jalankan [`db/schema.sql`](db/schema.sql)
2. Isi data awal:
   - Jalankan [`db/seed.sql`](db/seed.sql)
3. Sesuaikan koneksi di [`src/main/resources/app.properties`](src/main/resources/app.properties)
4. Jalankan aplikasi:

```bash
mvn clean javafx:run
```

## Default Login
- `admin / admin123`
- `kasir / kasir123`
- `manager / manager123`

## Catatan
- Nomor transaksi format: `TRX-YYYYMMDD-XXXX`
- Printer thermal default `80mm` (nilai di config)
- Ubah `app.paper.size` ke `58` atau `80` untuk memilih template thermal
- Pilih printer spesifik dari menu Laporan -> Pengaturan Printer (disimpan ke `app.printer.name`)
- Topup min/max diatur melalui `app.topup.min` dan `app.topup.max`
- Transaksi POS, deposit, kulakan, dan stock opname diproses dalam transaksi DB (ACID)
"# kasir-pendem" 
