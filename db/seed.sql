INSERT IGNORE INTO supplier(id,nama,kontak,alamat) VALUES (1,'Supplier Umum','-','-');

INSERT IGNORE INTO santri(id,nis,nama,kelas,aktif,saldo) VALUES
(1,'NIS001','Ahmad Fulan','7A',1,50000),
(2,'NIS002','Budi Hasan','8B',1,25000);

INSERT IGNORE INTO barang(id,kode,barcode,nama,satuan,harga_beli_avg,harga_jual,ppn_persen,stok,stok_min) VALUES
(1,'BRG001','899100000001','Buku Tulis','pcs',3000,4000,11,100,10),
(2,'BRG002','899100000002','Pensil','pcs',1500,2500,0,80,10),
(3,'BRG003','899100000003','Air Mineral','botol',2000,3000,11,50,15);

INSERT IGNORE INTO users(id,username,password_hash,role) VALUES
(2,'kasir',SHA2('kasir123',256),'KASIR'),
(3,'manager',SHA2('manager123',256),'MANAGER');
