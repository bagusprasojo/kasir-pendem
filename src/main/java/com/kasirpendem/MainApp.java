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
import javafx.application.Platform;
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
import java.util.concurrent.atomic.AtomicReference;
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
            operasional.getChildren().add(menuButton("Deposit Topup", () -> openWindow("Deposit Topup", depositTopupForm(user), 960, 700)));
            operasional.getChildren().add(menuButton("Reversal Topup", () -> openWindow("Reversal Topup", reversalTopupForm(user), 960, 700)));
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

    private VBox depositTopupForm(User user) {
        final int pageSize = 10;
        Label msg = new Label();
        TextField search = new TextField();
        search.setPromptText("Cari NIS/Nama santri");

        ObservableList<SantriMasterRow> santriRows = FXCollections.observableArrayList();
        ObservableList<SantriMasterRow> allSantriRows = FXCollections.observableArrayList();
        TableView<SantriMasterRow> santriTable = new TableView<>(santriRows);
        santriTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        santriTable.setFixedCellSize(28);
        santriTable.setPrefHeight((pageSize * 28) + 80);
        Pagination santriPager = new Pagination(1, 0);

        TableColumn<SantriMasterRow, String> cNis = new TableColumn<>("NIS");
        cNis.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().nis()));
        TableColumn<SantriMasterRow, String> cNama = new TableColumn<>("Nama");
        cNama.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().nama()));
        TableColumn<SantriMasterRow, String> cKelas = new TableColumn<>("Kelas");
        cKelas.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().kelas()));
        TableColumn<SantriMasterRow, BigDecimal> cSaldo = new TableColumn<>("Saldo");
        cSaldo.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().saldo()));
        santriTable.getColumns().addAll(cNis, cNama, cKelas, cSaldo);

        AtomicReference<Long> selectedSantriId = new AtomicReference<>(null);
        Label selectedTitle = new Label("Belum ada santri dipilih");
        TextField santriInfo = new TextField();
        santriInfo.setEditable(false);
        TextField saldoNow = new TextField();
        saldoNow.setEditable(false);
        VBox selectedCard = new VBox(4, selectedTitle, santriInfo, saldoNow);
        selectedCard.setPadding(new Insets(8));
        selectedCard.setStyle("-fx-background-color: #fff4cc; -fx-border-color: #e6c200; -fx-border-radius: 6; -fx-background-radius: 6;");
        TextField amount = new TextField();
        amount.setPromptText("Nominal topup");
        DatePicker from = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker to = new DatePicker(LocalDate.now());

        ObservableList<WalletRow> ledgerRows = FXCollections.observableArrayList();
        ObservableList<WalletRow> allLedgerRows = FXCollections.observableArrayList();
        TableView<WalletRow> ledgerTable = new TableView<>(ledgerRows);
        ledgerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ledgerTable.setFixedCellSize(28);
        ledgerTable.setPrefHeight((pageSize * 28) + 80);
        Pagination ledgerPager = new Pagination(1, 0);

        TableColumn<WalletRow, Long> lId = new TableColumn<>("TxID");
        lId.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().id()));
        TableColumn<WalletRow, String> lTime = new TableColumn<>("Waktu");
        lTime.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().createdAt()));
        TableColumn<WalletRow, String> lType = new TableColumn<>("Tipe");
        lType.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().tipe()));
        TableColumn<WalletRow, BigDecimal> lNom = new TableColumn<>("Nominal");
        lNom.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().nominal()));
        TableColumn<WalletRow, BigDecimal> lSaldo = new TableColumn<>("Saldo Setelah");
        lSaldo.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().saldoSetelah()));
        TableColumn<WalletRow, String> lRef = new TableColumn<>("Ref");
        lRef.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().refNo()));
        ledgerTable.getColumns().addAll(lId, lTime, lType, lNom, lSaldo, lRef);

        Runnable renderSantriPage = () -> {
            int idx = santriPager.getCurrentPageIndex();
            int fromIdx = idx * pageSize;
            int toIdx = Math.min(fromIdx + pageSize, allSantriRows.size());
            if (fromIdx >= toIdx) santriRows.clear(); else santriRows.setAll(allSantriRows.subList(fromIdx, toIdx));
        };
        Runnable loadSantri = () -> {
            allSantriRows.setAll(service.listSantriMaster(search.getText()).stream().map(this::parseSantriMasterRow).toList());
            santriPager.setPageCount(Math.max(1, (int) Math.ceil(allSantriRows.size() / (double) pageSize)));
            santriPager.setCurrentPageIndex(0);
            renderSantriPage.run();
        };
        Runnable restoreSelectedSantri = () -> {
            Long currentId = selectedSantriId.get();
            if (currentId == null) return;
            SantriMasterRow found = allSantriRows.stream().filter(s -> s.id() == currentId).findFirst().orElse(null);
            if (found == null) return;
            int globalIndex = allSantriRows.indexOf(found);
            int targetPage = globalIndex / pageSize;
            if (targetPage >= 0 && targetPage < santriPager.getPageCount()) {
                santriPager.setCurrentPageIndex(targetPage);
                renderSantriPage.run();
                santriTable.getSelectionModel().select(found);
            }
            santriInfo.setText(found.nis() + " - " + found.nama() + " (" + found.kelas() + ")");
            saldoNow.setText(found.saldo().toPlainString());
            selectedTitle.setText("Santri terpilih");
            selectedCard.setStyle("-fx-background-color: #e8f7ee; -fx-border-color: #2f855a; -fx-border-radius: 6; -fx-background-radius: 6;");
        };
        santriPager.currentPageIndexProperty().addListener((o, a, b) -> renderSantriPage.run());
        search.textProperty().addListener((o, a, b) -> loadSantri.run());

        Runnable renderLedgerPage = () -> {
            int idx = ledgerPager.getCurrentPageIndex();
            int fromIdx = idx * pageSize;
            int toIdx = Math.min(fromIdx + pageSize, allLedgerRows.size());
            if (fromIdx >= toIdx) ledgerRows.clear(); else ledgerRows.setAll(allLedgerRows.subList(fromIdx, toIdx));
        };
        Runnable loadLedger = () -> {
            if (selectedSantriId.get() == null) {
                allLedgerRows.clear();
                ledgerRows.clear();
                ledgerPager.setPageCount(1);
                return;
            }
            allLedgerRows.setAll(service.listWalletBySantri(selectedSantriId.get(), from.getValue(), to.getValue()).stream().map(this::parseWalletRow).toList());
            ledgerPager.setPageCount(Math.max(1, (int) Math.ceil(allLedgerRows.size() / (double) pageSize)));
            ledgerPager.setCurrentPageIndex(0);
            renderLedgerPage.run();
        };
        ledgerPager.currentPageIndexProperty().addListener((o, a, b) -> renderLedgerPage.run());

        santriTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, v) -> {
            if (v == null) return;
            selectedSantriId.set(v.id());
            selectedTitle.setText("Santri terpilih");
            santriInfo.setText(v.nis() + " - " + v.nama() + " (" + v.kelas() + ")");
            saldoNow.setText(v.saldo().toPlainString());
            selectedCard.setStyle("-fx-background-color: #e8f7ee; -fx-border-color: #2f855a; -fx-border-radius: 6; -fx-background-radius: 6;");
            loadLedger.run();
        });
        Button batalPilihSantri = new Button("Batal Pilih Santri");
        batalPilihSantri.setOnAction(e -> {
            selectedSantriId.set(null);
            santriTable.getSelectionModel().clearSelection();
            selectedTitle.setText("Belum ada santri dipilih");
            santriInfo.clear();
            saldoNow.clear();
            selectedCard.setStyle("-fx-background-color: #fff4cc; -fx-border-color: #e6c200; -fx-border-radius: 6; -fx-background-radius: 6;");
            allLedgerRows.clear();
            ledgerRows.clear();
            ledgerPager.setPageCount(1);
        });
        Button refresh = new Button("Refresh (F5)");
        refresh.setOnAction(e -> {
            loadSantri.run();
            loadLedger.run();
        });
        Button baru = new Button("Baru (Ctrl+N)");
        baru.setOnAction(e -> {
            amount.clear();
            msg.setText("");
        });
        Button topup = new Button("Proses Topup (Ctrl+T)");
        topup.setOnAction(e -> {
            try {
                if (selectedSantriId.get() == null) {
                    msg.setText("Pilih santri terlebih dahulu");
                    return;
                }
                String res = service.processTopup(selectedSantriId.get(), parseMoney(amount.getText(), "Nominal"), user.id());
                msg.setText(res);
                loadSantri.run();
                restoreSelectedSantri.run();
                loadLedger.run();
                if (res.startsWith("Topup sukses")) {
                    amount.clear();
                    amount.requestFocus();
                }
            } catch (Exception ex) {
                msg.setText("Validasi gagal: " + ex.getMessage());
            }
        });
        HBox santriPagerBox = new HBox(santriPager);
        santriPagerBox.setStyle("-fx-alignment: center-right;");
        VBox listPanel = new VBox(8, new HBox(8, search, refresh), santriTable, santriPagerBox);
        listPanel.setPadding(new Insets(10));
        listPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(8); formGrid.setVgap(8);
        formGrid.addRow(0, new Label("Nominal Topup"), amount);

        VBox formPanel = new VBox(8, selectedCard, formGrid, new HBox(8, baru, batalPilihSantri, topup), msg);
        formPanel.setPadding(new Insets(10));
        formPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        Button applyLedgerFilter = new Button("Terapkan Filter");
        applyLedgerFilter.setOnAction(e -> loadLedger.run());
        HBox ledgerPagerBox = new HBox(ledgerPager);
        ledgerPagerBox.setStyle("-fx-alignment: center-right;");
        VBox ledgerPanel = new VBox(8, new Label("Riwayat Deposit"), new HBox(8, new Label("Dari"), from, new Label("Sampai"), to, applyLedgerFilter), ledgerTable, ledgerPagerBox);
        ledgerPanel.setPadding(new Insets(10));
        ledgerPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        HBox topRow = new HBox(10, listPanel, formPanel);
        HBox.setHgrow(listPanel, Priority.ALWAYS);
        HBox.setHgrow(formPanel, Priority.ALWAYS);
        listPanel.setPrefWidth(560);
        formPanel.setPrefWidth(460);

        VBox box = new VBox(10, new Label("Deposit Topup"), topRow, ledgerPanel);
        box.setPadding(new Insets(10));
        box.setFocusTraversable(true);
        box.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.N) {
                baru.fire();
            } else if (e.isControlDown() && e.getCode() == KeyCode.F) {
                search.requestFocus();
            } else if (e.isControlDown() && e.getCode() == KeyCode.T) {
                topup.fire();
            } else if (e.getCode() == KeyCode.F5) {
                refresh.fire();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                batalPilihSantri.fire();
            }
        });

        loadSantri.run();
        return box;
    }

    private VBox reversalTopupForm(User user) {
        final int pageSize = 10;
        Label msg = new Label();
        TextField search = new TextField();
        search.setPromptText("Cari NIS/Nama santri");
        DatePicker from = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker to = new DatePicker(LocalDate.now());

        ObservableList<SantriMasterRow> santriRows = FXCollections.observableArrayList();
        ObservableList<SantriMasterRow> allSantriRows = FXCollections.observableArrayList();
        TableView<SantriMasterRow> santriTable = new TableView<>(santriRows);
        santriTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        santriTable.setFixedCellSize(28);
        santriTable.setPrefHeight((pageSize * 28) + 80);
        Pagination santriPager = new Pagination(1, 0);

        TableColumn<SantriMasterRow, String> cNis = new TableColumn<>("NIS");
        cNis.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().nis()));
        TableColumn<SantriMasterRow, String> cNama = new TableColumn<>("Nama");
        cNama.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().nama()));
        TableColumn<SantriMasterRow, String> cKelas = new TableColumn<>("Kelas");
        cKelas.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().kelas()));
        TableColumn<SantriMasterRow, BigDecimal> cSaldo = new TableColumn<>("Saldo");
        cSaldo.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().saldo()));
        santriTable.getColumns().addAll(cNis, cNama, cKelas, cSaldo);

        AtomicReference<Long> selectedSantriId = new AtomicReference<>(null);
        Label selectedTitle = new Label("Belum ada santri dipilih");
        TextField santriInfo = new TextField();
        santriInfo.setEditable(false);
        VBox selectedCard = new VBox(4, selectedTitle, santriInfo);
        selectedCard.setPadding(new Insets(8));
        selectedCard.setStyle("-fx-background-color: #fff4cc; -fx-border-color: #e6c200; -fx-border-radius: 6; -fx-background-radius: 6;");
        TextField walletId = new TextField();
        walletId.setEditable(false);
        TextField reason = new TextField();
        reason.setPromptText("Alasan reversal");

        ObservableList<TopupReversalRow> ledgerRows = FXCollections.observableArrayList();
        ObservableList<TopupReversalRow> allLedgerRows = FXCollections.observableArrayList();
        TableView<TopupReversalRow> ledgerTable = new TableView<>(ledgerRows);
        ledgerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ledgerTable.setFixedCellSize(28);
        ledgerTable.setPrefHeight((pageSize * 28) + 80);
        Pagination ledgerPager = new Pagination(1, 0);
        TableColumn<TopupReversalRow, Long> lId = new TableColumn<>("TxID");
        lId.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().id()));
        TableColumn<TopupReversalRow, String> lTime = new TableColumn<>("Waktu");
        lTime.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().createdAt()));
        TableColumn<TopupReversalRow, BigDecimal> lNom = new TableColumn<>("Nominal");
        lNom.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().nominal()));
        TableColumn<TopupReversalRow, BigDecimal> lSaldo = new TableColumn<>("Saldo Setelah");
        lSaldo.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().saldoSetelah()));
        TableColumn<TopupReversalRow, String> lStatus = new TableColumn<>("Status");
        lStatus.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().status()));
        ledgerTable.getColumns().addAll(lId, lTime, lNom, lSaldo, lStatus);

        Runnable renderSantriPage = () -> {
            int idx = santriPager.getCurrentPageIndex();
            int fromIdx = idx * pageSize;
            int toIdx = Math.min(fromIdx + pageSize, allSantriRows.size());
            if (fromIdx >= toIdx) santriRows.clear(); else santriRows.setAll(allSantriRows.subList(fromIdx, toIdx));
        };
        Runnable loadSantri = () -> {
            allSantriRows.setAll(service.listSantriMaster(search.getText()).stream().map(this::parseSantriMasterRow).toList());
            santriPager.setPageCount(Math.max(1, (int) Math.ceil(allSantriRows.size() / (double) pageSize)));
            santriPager.setCurrentPageIndex(0);
            renderSantriPage.run();
        };
        santriPager.currentPageIndexProperty().addListener((o, a, b) -> renderSantriPage.run());
        search.textProperty().addListener((o, a, b) -> loadSantri.run());

        Runnable renderLedgerPage = () -> {
            int idx = ledgerPager.getCurrentPageIndex();
            int fromIdx = idx * pageSize;
            int toIdx = Math.min(fromIdx + pageSize, allLedgerRows.size());
            if (fromIdx >= toIdx) ledgerRows.clear(); else ledgerRows.setAll(allLedgerRows.subList(fromIdx, toIdx));
        };
        Runnable loadTopupLedger = () -> {
            if (selectedSantriId.get() == null) {
                allLedgerRows.clear();
                ledgerRows.clear();
                ledgerPager.setPageCount(1);
                return;
            }
            allLedgerRows.setAll(
                    service.listTopupForReversal(selectedSantriId.get(), from.getValue(), to.getValue()).stream()
                            .map(this::parseTopupReversalRow)
                            .toList()
            );
            ledgerPager.setPageCount(Math.max(1, (int) Math.ceil(allLedgerRows.size() / (double) pageSize)));
            ledgerPager.setCurrentPageIndex(0);
            renderLedgerPage.run();
        };
        ledgerPager.currentPageIndexProperty().addListener((o, a, b) -> renderLedgerPage.run());

        santriTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, v) -> {
            if (v == null) return;
            selectedSantriId.set(v.id());
            selectedTitle.setText("Santri terpilih");
            santriInfo.setText(v.nis() + " - " + v.nama() + " (" + v.kelas() + ")");
            selectedCard.setStyle("-fx-background-color: #e8f7ee; -fx-border-color: #2f855a; -fx-border-radius: 6; -fx-background-radius: 6;");
            loadTopupLedger.run();
        });
        Button batalPilihSantri = new Button("Batal Pilih Santri");
        batalPilihSantri.setOnAction(e -> {
            selectedSantriId.set(null);
            santriTable.getSelectionModel().clearSelection();
            selectedTitle.setText("Belum ada santri dipilih");
            santriInfo.clear();
            selectedCard.setStyle("-fx-background-color: #fff4cc; -fx-border-color: #e6c200; -fx-border-radius: 6; -fx-background-radius: 6;");
            walletId.clear();
            reason.clear();
            allLedgerRows.clear();
            ledgerRows.clear();
            ledgerPager.setPageCount(1);
        });
        ledgerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, v) -> {
            if (v == null) return;
            if (!"AVAILABLE".equalsIgnoreCase(v.status())) {
                msg.setText("Transaksi ini sudah direversal dan tidak bisa diproses ulang");
                Platform.runLater(() -> {
                    ledgerTable.getSelectionModel().clearSelection();
                    walletId.clear();
                });
                return;
            }
            walletId.setText(String.valueOf(v.id()));
        });

        Button refresh = new Button("Refresh (F5)");
        refresh.setOnAction(e -> {
            loadSantri.run();
            loadTopupLedger.run();
        });
        Button baru = new Button("Baru (Ctrl+N)");
        baru.setOnAction(e -> {
            walletId.clear();
            reason.clear();
            msg.setText("");
            ledgerTable.getSelectionModel().clearSelection();
        });
        Button reverse = new Button("Proses Reversal (Ctrl+R)");
        reverse.setOnAction(e -> {
            if (reverse.isDisabled()) return;
            reverse.setDisable(true);
            try {
                if (walletId.getText().isBlank()) {
                    msg.setText("Pilih transaksi TOPUP untuk reversal");
                    return;
                }
                if (reason.getText().isBlank()) {
                    msg.setText("Alasan reversal wajib diisi");
                    return;
                }
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Batalkan topup wallet ID " + walletId.getText() + "?", ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Konfirmasi Reversal");
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.YES) {
                        String res = service.reverseTopup(parseLong(walletId.getText(), "Wallet ID"), reason.getText().trim(), user.id());
                        msg.setText(res);
                        loadSantri.run();
                        loadTopupLedger.run();
                        if (res.startsWith("Pembatalan topup berhasil")) {
                            reason.clear();
                            walletId.clear();
                            ledgerTable.getSelectionModel().clearSelection();
                            reason.requestFocus();
                        }
                    }
                });
            } catch (Exception ex) {
                msg.setText("Validasi gagal: " + ex.getMessage());
            } finally {
                reverse.setDisable(false);
            }
        });

        HBox santriPagerBox = new HBox(santriPager);
        santriPagerBox.setStyle("-fx-alignment: center-right;");
        VBox santriPanel = new VBox(8, new HBox(8, search, refresh), santriTable, santriPagerBox);
        santriPanel.setPadding(new Insets(10));
        santriPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(8); formGrid.setVgap(8);
        formGrid.addRow(0, new Label("Wallet Tx ID"), walletId);
        formGrid.addRow(1, new Label("Alasan Reversal"), reason);

        VBox formPanel = new VBox(8, selectedCard, formGrid, new HBox(8, baru, batalPilihSantri, reverse), msg);
        formPanel.setPadding(new Insets(10));
        formPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        Button applyTopupFilter = new Button("Terapkan Filter");
        applyTopupFilter.setOnAction(e -> loadTopupLedger.run());
        HBox ledgerPagerBox = new HBox(ledgerPager);
        ledgerPagerBox.setStyle("-fx-alignment: center-right;");
        VBox ledgerPanel = new VBox(8, new Label("Daftar TOPUP (untuk reversal)"), new HBox(8, new Label("Dari"), from, new Label("Sampai"), to, applyTopupFilter), ledgerTable, ledgerPagerBox);
        ledgerPanel.setPadding(new Insets(10));
        ledgerPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        HBox topRow = new HBox(10, santriPanel, formPanel);
        HBox.setHgrow(santriPanel, Priority.ALWAYS);
        HBox.setHgrow(formPanel, Priority.ALWAYS);
        santriPanel.setPrefWidth(560);
        formPanel.setPrefWidth(460);

        VBox box = new VBox(10, new Label("Reversal Topup"), topRow, ledgerPanel);
        box.setPadding(new Insets(10));
        box.setFocusTraversable(true);
        box.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.N) {
                baru.fire();
            } else if (e.isControlDown() && e.getCode() == KeyCode.F) {
                search.requestFocus();
            } else if (e.isControlDown() && e.getCode() == KeyCode.R) {
                reverse.fire();
            } else if (e.getCode() == KeyCode.F5) {
                refresh.fire();
            } else if (e.getCode() == KeyCode.DELETE) {
                ledgerTable.getSelectionModel().clearSelection();
                walletId.clear();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                batalPilihSantri.fire();
            }
        });

        loadSantri.run();
        return box;
    }

    private VBox inventoryForm(User user) {
        final int pageSize = 10;
        AtomicReference<Long> selectedBarangId = new AtomicReference<>(null);
        Label msg = new Label();

        TextField search = new TextField();
        search.setPromptText("Cari kode/nama/barcode barang");
        ObservableList<InventoryBarangRow> rows = FXCollections.observableArrayList();
        ObservableList<InventoryBarangRow> allRows = FXCollections.observableArrayList();
        TableView<InventoryBarangRow> table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(28);
        table.setPrefHeight((pageSize * 28) + 120);
        Pagination pager = new Pagination(1, 0);

        TableColumn<InventoryBarangRow, String> cKode = new TableColumn<>("Kode");
        cKode.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().kode()));
        TableColumn<InventoryBarangRow, String> cNama = new TableColumn<>("Nama");
        cNama.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().nama()));
        TableColumn<InventoryBarangRow, String> cSatuan = new TableColumn<>("Satuan");
        cSatuan.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().satuan()));
        TableColumn<InventoryBarangRow, BigDecimal> cStok = new TableColumn<>("Stok");
        cStok.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().stok()));
        TableColumn<InventoryBarangRow, BigDecimal> cStokMin = new TableColumn<>("Stok Min");
        cStokMin.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().stokMin()));
        table.getColumns().addAll(cKode, cNama, cSatuan, cStok, cStokMin);

        Label selectedTitle = new Label("Belum ada barang dipilih");
        TextField selectedBarang = new TextField();
        selectedBarang.setEditable(false);
        VBox selectedCard = new VBox(4, selectedTitle, selectedBarang);
        selectedCard.setPadding(new Insets(8));
        selectedCard.setStyle("-fx-background-color: #fff4cc; -fx-border-color: #e6c200; -fx-border-radius: 6; -fx-background-radius: 6;");

        TextField qty = new TextField();
        qty.setPromptText("Qty keluar");
        ComboBox<String> kategori = new ComboBox<>();
        kategori.getItems().addAll("RUSAK", "HILANG", "PEMAKAIAN_INTERNAL");
        kategori.getSelectionModel().selectFirst();
        TextField note = new TextField();
        note.setPromptText("Catatan (opsional)");

        DatePicker from = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker to = new DatePicker(LocalDate.now());
        CheckBox showAllMoves = new CheckBox("Tampilkan semua barang (audit)");
        TextField koreksiMovementId = new TextField();
        koreksiMovementId.setEditable(false);
        TextField koreksiReason = new TextField();
        koreksiReason.setPromptText("Alasan koreksi");
        ObservableList<StockMoveRow> moveRows = FXCollections.observableArrayList();
        ObservableList<StockMoveRow> allMoveRows = FXCollections.observableArrayList();
        TableView<StockMoveRow> moveTable = new TableView<>(moveRows);
        moveTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        moveTable.setFixedCellSize(28);
        moveTable.setPrefHeight((pageSize * 28) + 120);
        Pagination movePager = new Pagination(1, 0);

        TableColumn<StockMoveRow, String> mTime = new TableColumn<>("Waktu");
        mTime.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().createdAt()));
        TableColumn<StockMoveRow, String> mKode = new TableColumn<>("Kode");
        mKode.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().kode()));
        TableColumn<StockMoveRow, String> mNama = new TableColumn<>("Nama");
        mNama.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().nama()));
        TableColumn<StockMoveRow, String> mTipe = new TableColumn<>("Tipe");
        mTipe.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().tipe()));
        TableColumn<StockMoveRow, String> mKategori = new TableColumn<>("Kategori");
        mKategori.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().kategori()));
        TableColumn<StockMoveRow, BigDecimal> mQty = new TableColumn<>("Qty");
        mQty.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().qty()));
        TableColumn<StockMoveRow, String> mNote = new TableColumn<>("Note");
        mNote.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().note()));
        TableColumn<StockMoveRow, String> mUser = new TableColumn<>("User");
        mUser.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().username()));
        moveTable.getColumns().addAll(mTime, mKode, mNama, mTipe, mKategori, mQty, mNote, mUser);
        moveTable.getSelectionModel().selectedItemProperty().addListener((o, a, v) -> {
            if (v == null) return;
            koreksiMovementId.setText(String.valueOf(v.id()));
        });

        Runnable renderBarangPage = () -> {
            int idx = pager.getCurrentPageIndex();
            int f = idx * pageSize;
            int t = Math.min(f + pageSize, allRows.size());
            if (f >= t) rows.clear(); else rows.setAll(allRows.subList(f, t));
        };
        Runnable loadBarang = () -> {
            allRows.setAll(service.listBarangInventory(search.getText()).stream().map(this::parseInventoryBarangRow).toList());
            pager.setPageCount(Math.max(1, (int) Math.ceil(allRows.size() / (double) pageSize)));
            pager.setCurrentPageIndex(0);
            renderBarangPage.run();
        };
        pager.currentPageIndexProperty().addListener((o, a, b) -> renderBarangPage.run());
        search.textProperty().addListener((o, a, b) -> loadBarang.run());
        Runnable renderMovePage = () -> {
            int idx = movePager.getCurrentPageIndex();
            int f = idx * pageSize;
            int t = Math.min(f + pageSize, allMoveRows.size());
            if (f >= t) moveRows.clear(); else moveRows.setAll(allMoveRows.subList(f, t));
        };
        Runnable loadMoves = () -> {
            if (!showAllMoves.isSelected() && selectedBarangId.get() == null) {
                allMoveRows.clear();
                moveRows.clear();
                movePager.setPageCount(1);
                return;
            }
            List<String> data = showAllMoves.isSelected()
                    ? service.listStockMovement(from.getValue(), to.getValue())
                    : service.listStockMovementByBarang(selectedBarangId.get(), from.getValue(), to.getValue());
            allMoveRows.setAll(data.stream().map(this::parseStockMoveRow).toList());
            movePager.setPageCount(Math.max(1, (int) Math.ceil(allMoveRows.size() / (double) pageSize)));
            movePager.setCurrentPageIndex(0);
            renderMovePage.run();
        };
        movePager.currentPageIndexProperty().addListener((o, a, b) -> renderMovePage.run());
        showAllMoves.selectedProperty().addListener((o, a, b) -> loadMoves.run());

        table.getSelectionModel().selectedItemProperty().addListener((o, a, v) -> {
            if (v == null) return;
            selectedBarangId.set(v.id());
            selectedTitle.setText("Barang terpilih");
            selectedBarang.setText(v.kode() + " - " + v.nama() + " | stok " + v.stok() + " " + v.satuan());
            selectedCard.setStyle("-fx-background-color: #e8f7ee; -fx-border-color: #2f855a; -fx-border-radius: 6; -fx-background-radius: 6;");
            if (!showAllMoves.isSelected()) {
                loadMoves.run();
            }
        });

        Button baru = new Button("Baru (Ctrl+N)");
        baru.setOnAction(e -> {
            qty.clear();
            note.clear();
            msg.setText("");
        });
        Button batal = new Button("Batal Pilih Barang");
        batal.setOnAction(e -> {
            selectedBarangId.set(null);
            table.getSelectionModel().clearSelection();
            selectedTitle.setText("Belum ada barang dipilih");
            selectedBarang.clear();
            selectedCard.setStyle("-fx-background-color: #fff4cc; -fx-border-color: #e6c200; -fx-border-radius: 6; -fx-background-radius: 6;");
            if (!showAllMoves.isSelected()) {
                loadMoves.run();
            }
        });
        Button proses = new Button("Proses (Ctrl+S)");
        proses.setOnAction(e -> {
            try {
                if (selectedBarangId.get() == null) {
                    msg.setText("Pilih barang terlebih dahulu");
                    return;
                }
                String res = service.stockOutNonSales(selectedBarangId.get(), parseMoney(qty.getText(), "Qty"), kategori.getValue(), note.getText().trim(), user.id());
                msg.setText(res);
                loadBarang.run();
                loadMoves.run();
                if (res.startsWith("Stok keluar non-penjualan tercatat")) {
                    qty.clear();
                    note.clear();
                    qty.requestFocus();
                }
            } catch (Exception ex) {
                msg.setText("Validasi gagal: " + ex.getMessage());
            }
        });
        Button refresh = new Button("Refresh (F5)");
        refresh.setOnAction(e -> {
            loadBarang.run();
            loadMoves.run();
        });
        Button applyFilter = new Button("Terapkan Filter");
        applyFilter.setOnAction(e -> loadMoves.run());
        Button koreksiBtn = new Button("Koreksi Transaksi");
        koreksiBtn.setDisable(true);
        moveTable.getSelectionModel().selectedItemProperty().addListener((o, a, v) -> {
            if (v == null) {
                koreksiBtn.setDisable(true);
                return;
            }
            boolean allowed = "RUSAK".equalsIgnoreCase(v.kategori())
                    || "HILANG".equalsIgnoreCase(v.kategori())
                    || "PEMAKAIAN_INTERNAL".equalsIgnoreCase(v.kategori());
            koreksiBtn.setDisable(!allowed);
            if (!allowed) {
                msg.setText("Mutasi kategori " + v.kategori() + " tidak bisa dikoreksi di form Inventori");
            }
        });
        koreksiBtn.setOnAction(e -> {
            try {
                if (koreksiMovementId.getText().isBlank()) {
                    msg.setText("Pilih mutasi di tabel riwayat untuk dikoreksi");
                    return;
                }
                if (koreksiReason.getText().isBlank()) {
                    msg.setText("Alasan koreksi wajib diisi");
                    return;
                }
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Buat transaksi koreksi untuk mutasi ID " + koreksiMovementId.getText() + "?", ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("Konfirmasi Koreksi");
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.YES) {
                        String res = service.koreksiStockMovement(parseLong(koreksiMovementId.getText(), "Movement ID"), koreksiReason.getText().trim(), user.id());
                        msg.setText(res);
                        loadBarang.run();
                        loadMoves.run();
                        if (res.startsWith("Koreksi berhasil")) {
                            koreksiMovementId.clear();
                            koreksiReason.clear();
                            moveTable.getSelectionModel().clearSelection();
                        }
                    }
                });
            } catch (Exception ex) {
                msg.setText("Koreksi gagal: " + ex.getMessage());
            }
        });

        HBox pagerBox = new HBox(pager);
        pagerBox.setStyle("-fx-alignment: center-right;");
        VBox listPanel = new VBox(8, new HBox(8, search, refresh), table, pagerBox);
        listPanel.setPadding(new Insets(10));
        listPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(8); formGrid.setVgap(8);
        formGrid.addRow(0, new Label("Qty keluar"), qty);
        formGrid.addRow(1, new Label("Kategori"), kategori);
        formGrid.addRow(2, new Label("Catatan"), note);
        VBox formPanel = new VBox(8, selectedCard, formGrid, new HBox(8, baru, batal, proses), msg);
        formPanel.setPadding(new Insets(10));
        formPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        HBox topRow = new HBox(10, listPanel, formPanel);
        HBox.setHgrow(listPanel, Priority.ALWAYS);
        HBox.setHgrow(formPanel, Priority.ALWAYS);
        listPanel.setPrefWidth(560);
        formPanel.setPrefWidth(460);

        HBox movePagerBox = new HBox(movePager);
        movePagerBox.setStyle("-fx-alignment: center-right;");
        VBox movePanel = new VBox(
                8,
                new Label("Riwayat Mutasi Stok"),
                new HBox(8, new Label("Dari"), from, new Label("Sampai"), to, showAllMoves, applyFilter),
                moveTable,
                movePagerBox,
                new Separator(),
                new Label("Koreksi Transaksi"),
                new HBox(8, new Label("Mutasi ID"), koreksiMovementId, new Label("Alasan"), koreksiReason, koreksiBtn)
        );
        movePanel.setPadding(new Insets(10));
        movePanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        VBox box = new VBox(10, new Label("Inventori"), topRow, movePanel);
        box.setPadding(new Insets(10));
        box.setFocusTraversable(true);
        box.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.F) search.requestFocus();
            else if (e.isControlDown() && e.getCode() == KeyCode.N) baru.fire();
            else if (e.isControlDown() && e.getCode() == KeyCode.S) proses.fire();
            else if (e.getCode() == KeyCode.F5) refresh.fire();
            else if (e.getCode() == KeyCode.ESCAPE) batal.fire();
        });

        loadBarang.run();
        loadMoves.run();
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
        final int pageSize = 10;
        Label msg = new Label();
        TextField search = new TextField();
        search.setPromptText("Cari kode/barcode/nama barang");

        ObservableList<BarangMasterRow> rows = FXCollections.observableArrayList();
        ObservableList<BarangMasterRow> allRows = FXCollections.observableArrayList();
        TableView<BarangMasterRow> table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(28);
        table.setPrefHeight((pageSize * 28) + 130);
        Pagination pagination = new Pagination(1, 0);

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
        table.getColumns().addAll(cKode, cBarcode, cNama, cSatuan, cHarga, cPpn, cStok, cStokMin);

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

        Runnable renderCurrentPage = () -> {
            int pageIndex = pagination.getCurrentPageIndex();
            int from = pageIndex * pageSize;
            int to = Math.min(from + pageSize, allRows.size());
            if (from >= to) {
                rows.clear();
            } else {
                rows.setAll(allRows.subList(from, to));
            }
            table.refresh();
        };

        Runnable loadRows = () -> {
            allRows.setAll(service.listBarangMaster(search.getText()).stream().map(this::parseBarangMasterRow).toList());
            int pageCount = Math.max(1, (int) Math.ceil(allRows.size() / (double) pageSize));
            pagination.setPageCount(pageCount);
            pagination.setCurrentPageIndex(0);
            renderCurrentPage.run();
        };

        pagination.currentPageIndexProperty().addListener((obs, oldV, newV) -> renderCurrentPage.run());

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
        g.addRow(0, new Label("Kode"), kode);
        g.addRow(1, new Label("Barcode"), barcode);
        g.addRow(2, new Label("Nama"), nama);
        g.addRow(3, new Label("Satuan"), satuan);
        g.addRow(4, new Label("Harga Jual"), harga);
        g.addRow(5, new Label("PPN %"), ppn);
        g.addRow(6, new Label("Stok Minimum"), stokMin);

        HBox listPagerWrap = new HBox(pagination);
        listPagerWrap.setStyle("-fx-alignment: center-right;");
        VBox listPanel = new VBox(8,
                new HBox(8, search, refresh),
                table,
                listPagerWrap
        );
        listPanel.setPadding(new Insets(10));
        listPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        VBox inputPanel = new VBox(8,
                g,
                new HBox(8, baru, simpan, update, hapus),
                msg
        );
        inputPanel.setPadding(new Insets(10));
        inputPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        VBox box = new VBox(10,
                new Label("Master Barang"),
                listPanel,
                inputPanel
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
        final int pageSize = 10;
        Label msg = new Label();
        TextField search = new TextField();
        search.setPromptText("Cari NIS/nama/kelas santri");

        ObservableList<SantriMasterRow> rows = FXCollections.observableArrayList();
        ObservableList<SantriMasterRow> allRows = FXCollections.observableArrayList();
        TableView<SantriMasterRow> table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(28);
        table.setPrefHeight((pageSize * 28) + 130);
        Pagination pagination = new Pagination(1, 0);

        TableColumn<SantriMasterRow, String> cNis = new TableColumn<>("NIS");
        cNis.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().nis()));
        TableColumn<SantriMasterRow, String> cNama = new TableColumn<>("Nama");
        cNama.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().nama()));
        TableColumn<SantriMasterRow, String> cKelas = new TableColumn<>("Kelas");
        cKelas.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().kelas()));
        TableColumn<SantriMasterRow, Boolean> cAktif = new TableColumn<>("Aktif");
        cAktif.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().aktif()));
        TableColumn<SantriMasterRow, BigDecimal> cSaldo = new TableColumn<>("Saldo");
        cSaldo.setCellValueFactory(v -> new SimpleObjectProperty<>(v.getValue().saldo()));
        table.getColumns().addAll(cNis, cNama, cKelas, cAktif, cSaldo);

        TextField id = new TextField();
        id.setVisible(false);
        id.setManaged(false);
        TextField nis = new TextField();
        TextField nama = new TextField();
        TextField kelas = new TextField();
        CheckBox aktif = new CheckBox("Aktif");
        aktif.setSelected(true);

        Runnable clearForm = () -> {
            id.clear();
            nis.clear();
            nama.clear();
            kelas.clear();
            aktif.setSelected(true);
            msg.setText("");
            nis.requestFocus();
        };

        Runnable renderCurrentPage = () -> {
            int pageIndex = pagination.getCurrentPageIndex();
            int from = pageIndex * pageSize;
            int to = Math.min(from + pageSize, allRows.size());
            if (from >= to) rows.clear();
            else rows.setAll(allRows.subList(from, to));
            table.refresh();
        };

        Runnable loadRows = () -> {
            allRows.setAll(service.listSantriMaster(search.getText()).stream().map(this::parseSantriMasterRow).toList());
            int pageCount = Math.max(1, (int) Math.ceil(allRows.size() / (double) pageSize));
            pagination.setPageCount(pageCount);
            pagination.setCurrentPageIndex(0);
            renderCurrentPage.run();
        };

        pagination.currentPageIndexProperty().addListener((obs, oldV, newV) -> renderCurrentPage.run());
        search.textProperty().addListener((obs, oldV, v) -> loadRows.run());
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, v) -> {
            if (v == null) return;
            id.setText(String.valueOf(v.id()));
            nis.setText(v.nis());
            nama.setText(v.nama());
            kelas.setText(v.kelas());
            aktif.setSelected(v.aktif());
        });

        Button baru = new Button("Baru (Ctrl+N)");
        baru.setOnAction(e -> clearForm.run());
        Button simpan = new Button("Simpan (Ctrl+S)");
        simpan.setOnAction(e -> {
            try {
                validateSantriForm(nis, nama, kelas);
                String res = service.createSantri(nis.getText().trim(), nama.getText().trim(), kelas.getText().trim(), aktif.isSelected());
                msg.setText(res);
                loadRows.run();
                if (res.startsWith("Santri berhasil")) clearForm.run();
            } catch (Exception ex) {
                msg.setText("Validasi gagal: " + ex.getMessage());
            }
        });
        Button update = new Button("Update");
        update.setOnAction(e -> {
            try {
                if (id.getText().isBlank()) {
                    msg.setText("Pilih santri dari tabel untuk update");
                    return;
                }
                validateSantriForm(nis, nama, kelas);
                String res = service.updateSantri(parseLong(id.getText(), "ID"), nis.getText().trim(), nama.getText().trim(), kelas.getText().trim(), aktif.isSelected());
                msg.setText(res);
                loadRows.run();
            } catch (Exception ex) {
                msg.setText("Validasi gagal: " + ex.getMessage());
            }
        });
        Button hapus = new Button("Hapus (Del)");
        hapus.setOnAction(e -> {
            if (id.getText().isBlank()) {
                msg.setText("Pilih santri dari tabel untuk hapus");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Hapus santri ID " + id.getText() + "?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Konfirmasi Hapus");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    String res = service.deleteSantri(parseLong(id.getText(), "ID"));
                    msg.setText(res);
                    loadRows.run();
                    if (res.startsWith("Santri berhasil")) clearForm.run();
                }
            });
        });
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> loadRows.run());

        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8);
        g.addRow(0, new Label("NIS"), nis);
        g.addRow(1, new Label("Nama"), nama);
        g.addRow(2, new Label("Kelas"), kelas);
        g.addRow(3, new Label("Status"), aktif);

        HBox listPagerWrap = new HBox(pagination);
        listPagerWrap.setStyle("-fx-alignment: center-right;");
        VBox listPanel = new VBox(8,
                new HBox(8, search, refresh),
                table,
                listPagerWrap
        );
        listPanel.setPadding(new Insets(10));
        listPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        VBox inputPanel = new VBox(8,
                g,
                new HBox(8, baru, simpan, update, hapus),
                msg
        );
        inputPanel.setPadding(new Insets(10));
        inputPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        VBox box = new VBox(10, new Label("Master Santri"), listPanel, inputPanel);
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

    private VBox masterSupplierForm() {
        final int pageSize = 10;
        Label msg = new Label();
        TextField search = new TextField();
        search.setPromptText("Cari nama/kontak/alamat supplier");

        ObservableList<SupplierMasterRow> rows = FXCollections.observableArrayList();
        ObservableList<SupplierMasterRow> allRows = FXCollections.observableArrayList();
        TableView<SupplierMasterRow> table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(28);
        table.setPrefHeight((pageSize * 28) + 130);
        Pagination pagination = new Pagination(1, 0);

        TableColumn<SupplierMasterRow, String> cNama = new TableColumn<>("Nama");
        cNama.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().nama()));
        TableColumn<SupplierMasterRow, String> cKontak = new TableColumn<>("Kontak");
        cKontak.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().kontak()));
        TableColumn<SupplierMasterRow, String> cAlamat = new TableColumn<>("Alamat");
        cAlamat.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().alamat()));
        table.getColumns().addAll(cNama, cKontak, cAlamat);

        TextField id = new TextField();
        id.setVisible(false);
        id.setManaged(false);
        TextField nama = new TextField();
        TextField kontak = new TextField();
        TextField alamat = new TextField();

        Runnable clearForm = () -> {
            id.clear();
            nama.clear();
            kontak.clear();
            alamat.clear();
            msg.setText("");
            nama.requestFocus();
        };

        Runnable renderCurrentPage = () -> {
            int pageIndex = pagination.getCurrentPageIndex();
            int from = pageIndex * pageSize;
            int to = Math.min(from + pageSize, allRows.size());
            if (from >= to) rows.clear();
            else rows.setAll(allRows.subList(from, to));
            table.refresh();
        };

        Runnable loadRows = () -> {
            allRows.setAll(service.listSupplierMaster(search.getText()).stream().map(this::parseSupplierMasterRow).toList());
            int pageCount = Math.max(1, (int) Math.ceil(allRows.size() / (double) pageSize));
            pagination.setPageCount(pageCount);
            pagination.setCurrentPageIndex(0);
            renderCurrentPage.run();
        };

        pagination.currentPageIndexProperty().addListener((obs, oldV, newV) -> renderCurrentPage.run());
        search.textProperty().addListener((obs, oldV, v) -> loadRows.run());
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, v) -> {
            if (v == null) return;
            id.setText(String.valueOf(v.id()));
            nama.setText(v.nama());
            kontak.setText(v.kontak());
            alamat.setText(v.alamat());
        });

        Button baru = new Button("Baru (Ctrl+N)");
        baru.setOnAction(e -> clearForm.run());
        Button simpan = new Button("Simpan (Ctrl+S)");
        simpan.setOnAction(e -> {
            try {
                validateSupplierForm(nama);
                String res = service.createSupplier(nama.getText().trim(), kontak.getText().trim(), alamat.getText().trim());
                msg.setText(res);
                loadRows.run();
                if (res.startsWith("Supplier berhasil")) clearForm.run();
            } catch (Exception ex) {
                msg.setText("Validasi gagal: " + ex.getMessage());
            }
        });
        Button update = new Button("Update");
        update.setOnAction(e -> {
            try {
                if (id.getText().isBlank()) {
                    msg.setText("Pilih supplier dari tabel untuk update");
                    return;
                }
                validateSupplierForm(nama);
                String res = service.updateSupplier(parseLong(id.getText(), "ID"), nama.getText().trim(), kontak.getText().trim(), alamat.getText().trim());
                msg.setText(res);
                loadRows.run();
            } catch (Exception ex) {
                msg.setText("Validasi gagal: " + ex.getMessage());
            }
        });
        Button hapus = new Button("Hapus (Del)");
        hapus.setOnAction(e -> {
            if (id.getText().isBlank()) {
                msg.setText("Pilih supplier dari tabel untuk hapus");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Hapus supplier ID " + id.getText() + "?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Konfirmasi Hapus");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    String res = service.deleteSupplier(parseLong(id.getText(), "ID"));
                    msg.setText(res);
                    loadRows.run();
                    if (res.startsWith("Supplier berhasil")) clearForm.run();
                }
            });
        });
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> loadRows.run());

        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8);
        g.addRow(0, new Label("Nama"), nama);
        g.addRow(1, new Label("Kontak"), kontak);
        g.addRow(2, new Label("Alamat"), alamat);

        HBox listPagerWrap = new HBox(pagination);
        listPagerWrap.setStyle("-fx-alignment: center-right;");
        VBox listPanel = new VBox(8,
                new HBox(8, search, refresh),
                table,
                listPagerWrap
        );
        listPanel.setPadding(new Insets(10));
        listPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        VBox inputPanel = new VBox(8,
                g,
                new HBox(8, baru, simpan, update, hapus),
                msg
        );
        inputPanel.setPadding(new Insets(10));
        inputPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        VBox box = new VBox(10, new Label("Master Supplier"), listPanel, inputPanel);
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

    private VBox masterUserForm() {
        final int pageSize = 10;
        Label msg = new Label();
        TextField search = new TextField();
        search.setPromptText("Cari username/role");

        ObservableList<UserMasterRow> rows = FXCollections.observableArrayList();
        ObservableList<UserMasterRow> allRows = FXCollections.observableArrayList();
        TableView<UserMasterRow> table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(28);
        table.setPrefHeight((pageSize * 28) + 130);
        Pagination pagination = new Pagination(1, 0);

        TableColumn<UserMasterRow, String> cUsername = new TableColumn<>("Username");
        cUsername.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().username()));
        TableColumn<UserMasterRow, String> cRole = new TableColumn<>("Role");
        cRole.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().role()));
        table.getColumns().addAll(cUsername, cRole);

        TextField id = new TextField();
        id.setVisible(false);
        id.setManaged(false);
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<Role> role = new ComboBox<>();
        role.getItems().addAll(Role.ADMIN, Role.KASIR, Role.MANAGER);
        role.getSelectionModel().select(Role.KASIR);

        Runnable clearForm = () -> {
            id.clear();
            username.clear();
            password.clear();
            role.getSelectionModel().select(Role.KASIR);
            msg.setText("");
            username.requestFocus();
        };

        Runnable renderCurrentPage = () -> {
            int pageIndex = pagination.getCurrentPageIndex();
            int from = pageIndex * pageSize;
            int to = Math.min(from + pageSize, allRows.size());
            if (from >= to) rows.clear();
            else rows.setAll(allRows.subList(from, to));
            table.refresh();
        };

        Runnable loadRows = () -> {
            allRows.setAll(service.listUserMaster(search.getText()).stream().map(this::parseUserMasterRow).toList());
            int pageCount = Math.max(1, (int) Math.ceil(allRows.size() / (double) pageSize));
            pagination.setPageCount(pageCount);
            pagination.setCurrentPageIndex(0);
            renderCurrentPage.run();
        };

        pagination.currentPageIndexProperty().addListener((obs, oldV, newV) -> renderCurrentPage.run());
        search.textProperty().addListener((obs, oldV, v) -> loadRows.run());
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, v) -> {
            if (v == null) return;
            id.setText(String.valueOf(v.id()));
            username.setText(v.username());
            password.clear();
            role.setValue(Role.valueOf(v.role()));
        });

        Button baru = new Button("Baru (Ctrl+N)");
        baru.setOnAction(e -> clearForm.run());
        Button simpan = new Button("Simpan (Ctrl+S)");
        simpan.setOnAction(e -> {
            try {
                validateUserForm(username, password, true);
                String res = service.createUser(username.getText().trim(), password.getText(), role.getValue());
                msg.setText(res);
                loadRows.run();
                if (res.startsWith("User berhasil")) clearForm.run();
            } catch (Exception ex) {
                msg.setText("Validasi gagal: " + ex.getMessage());
            }
        });
        Button update = new Button("Update");
        update.setOnAction(e -> {
            try {
                if (id.getText().isBlank()) {
                    msg.setText("Pilih user dari tabel untuk update");
                    return;
                }
                validateUserForm(username, password, false);
                String res = service.updateUser(parseLong(id.getText(), "ID"), username.getText().trim(), password.getText(), role.getValue());
                msg.setText(res);
                loadRows.run();
            } catch (Exception ex) {
                msg.setText("Validasi gagal: " + ex.getMessage());
            }
        });
        Button hapus = new Button("Hapus (Del)");
        hapus.setOnAction(e -> {
            if (id.getText().isBlank()) {
                msg.setText("Pilih user dari tabel untuk hapus");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Hapus user ID " + id.getText() + "?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Konfirmasi Hapus");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    String res = service.deleteUser(parseLong(id.getText(), "ID"));
                    msg.setText(res);
                    loadRows.run();
                    if (res.startsWith("User berhasil")) clearForm.run();
                }
            });
        });
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> loadRows.run());

        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8);
        g.addRow(0, new Label("Username"), username);
        g.addRow(1, new Label("Password (kosongkan saat update jika tidak ganti)"), password);
        g.addRow(2, new Label("Role"), role);

        HBox listPagerWrap = new HBox(pagination);
        listPagerWrap.setStyle("-fx-alignment: center-right;");
        VBox listPanel = new VBox(8,
                new HBox(8, search, refresh),
                table,
                listPagerWrap
        );
        listPanel.setPadding(new Insets(10));
        listPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        VBox inputPanel = new VBox(8,
                g,
                new HBox(8, baru, simpan, update, hapus),
                msg
        );
        inputPanel.setPadding(new Insets(10));
        inputPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #d5dbe3; -fx-border-radius: 8; -fx-background-radius: 8;");

        VBox box = new VBox(10, new Label("Master User"), listPanel, inputPanel);
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

    private SantriMasterRow parseSantriMasterRow(String line) {
        String[] p = line.split("\\|");
        if (p.length < 6) throw new IllegalArgumentException("Format santri master tidak valid");
        return new SantriMasterRow(
                Long.parseLong(p[0].trim()),
                p[1].trim(),
                p[2].trim(),
                p[3].trim(),
                Boolean.parseBoolean(p[4].trim()),
                safeMoney(p[5].trim())
        );
    }

    private void validateSantriForm(TextField nis, TextField nama, TextField kelas) {
        if (nis.getText().isBlank()) throw new IllegalArgumentException("NIS wajib diisi");
        if (nama.getText().isBlank()) throw new IllegalArgumentException("Nama wajib diisi");
        if (kelas.getText().isBlank()) throw new IllegalArgumentException("Kelas wajib diisi");
    }

    private SupplierMasterRow parseSupplierMasterRow(String line) {
        String[] p = line.split("\\|");
        if (p.length < 4) throw new IllegalArgumentException("Format supplier master tidak valid");
        return new SupplierMasterRow(
                Long.parseLong(p[0].trim()),
                p[1].trim(),
                p[2].trim(),
                p[3].trim()
        );
    }

    private void validateSupplierForm(TextField nama) {
        if (nama.getText().isBlank()) throw new IllegalArgumentException("Nama supplier wajib diisi");
    }

    private UserMasterRow parseUserMasterRow(String line) {
        String[] p = line.split("\\|");
        if (p.length < 3) throw new IllegalArgumentException("Format user master tidak valid");
        return new UserMasterRow(
                Long.parseLong(p[0].trim()),
                p[1].trim(),
                p[2].trim()
        );
    }

    private void validateUserForm(TextField username, PasswordField password, boolean requirePassword) {
        if (username.getText().isBlank()) throw new IllegalArgumentException("Username wajib diisi");
        if (requirePassword && password.getText().isBlank()) {
            throw new IllegalArgumentException("Password wajib diisi");
        }
    }

    private WalletRow parseWalletRow(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 8) throw new IllegalArgumentException("Format wallet row tidak valid");
        return new WalletRow(
                Long.parseLong(p[0].trim()),
                p[1].trim(),
                p[2].trim(),
                safeMoney(p[3].trim()),
                safeMoney(p[4].trim()),
                p[5].trim(),
                p[6].trim(),
                p[7].trim()
        );
    }

    private TopupReversalRow parseTopupReversalRow(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 5) throw new IllegalArgumentException("Format topup reversal row tidak valid");
        return new TopupReversalRow(
                Long.parseLong(p[0].trim()),
                p[1].trim(),
                safeMoney(p[2].trim()),
                safeMoney(p[3].trim()),
                p[4].trim()
        );
    }

    private InventoryBarangRow parseInventoryBarangRow(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 6) throw new IllegalArgumentException("Format inventory barang tidak valid");
        return new InventoryBarangRow(
                Long.parseLong(p[0].trim()),
                p[1].trim(),
                p[2].trim(),
                p[3].trim(),
                safeMoney(p[4].trim()),
                safeMoney(p[5].trim())
        );
    }

    private StockMoveRow parseStockMoveRow(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 9) throw new IllegalArgumentException("Format stock movement tidak valid");
        return new StockMoveRow(
                Long.parseLong(p[0].trim()),
                p[1].trim(),
                p[2].trim(),
                p[3].trim(),
                p[4].trim(),
                p[5].trim(),
                safeMoney(p[6].trim()),
                p[7].trim(),
                p[8].trim()
        );
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

    private record SantriMasterRow(
            long id, String nis, String nama, String kelas, boolean aktif, BigDecimal saldo
    ) {}

    private record SupplierMasterRow(
            long id, String nama, String kontak, String alamat
    ) {}

    private record UserMasterRow(
            long id, String username, String role
    ) {}

    private record WalletRow(
            long id, String createdAt, String tipe, BigDecimal nominal, BigDecimal saldoSetelah,
            String refNo, String reason, String authorizedBy
    ) {}

    private record TopupReversalRow(
            long id, String createdAt, BigDecimal nominal, BigDecimal saldoSetelah, String status
    ) {}

    private record InventoryBarangRow(
            long id, String kode, String nama, String satuan, BigDecimal stok, BigDecimal stokMin
    ) {}

    private record StockMoveRow(
            long id, String createdAt, String kode, String nama, String tipe, String kategori, BigDecimal qty, String note, String username
    ) {}

    public static void main(String[] args) {
        launch(args);
    }

    @FunctionalInterface
    private interface ReportLoader {
        List<String> load(LocalDate from, LocalDate to);
    }
}
