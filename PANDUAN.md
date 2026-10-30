# Jejak Langkah: APK + Windows + sinkron

Satu aplikasi, tiga cara dibuka. Semua tersambung lewat internet.

- **HP Android**: APK dengan bubble bulat melayang di atas aplikasi lain
- **Windows**: aplikasi yang di-install dari Microsoft Edge
- **Sinkron**: lewat Firebase (gratis), memakai satu *kode sinkron* yang sama di semua perangkat

Total waktu sekitar 20–30 menit, cukup sekali saja. Ikuti urutannya dan jangan lompat.

---

## Langkah 1: Buat akun dan repo GitHub (5 menit)

1. Buka **github.com** dan daftar atau login.
2. Klik **+** di kanan atas, lalu **New repository**.
3. Isi **Repository name**: `jejak-langkah`. Pilih **Public**, lalu klik **Create repository**.
4. Di halaman repo kosong, klik tautan **uploading an existing file**.
5. Ekstrak file zip `jejak-langkah-app.zip` di laptop. Buka foldernya, **pilih semua isinya** (folder `.github`, `android`, `web`, file `PANDUAN.md`, `.gitignore`), lalu seret ke halaman GitHub.
   - Seret **isi foldernya**, bukan folder `jejak-langkah-app`-nya.
   - Pastikan folder `.github` ikut. Kalau tidak kelihatan di Windows, buka File Explorer, pilih **View**, lalu centang **Hidden items**.
6. Klik **Commit changes**.

✅ **Cek**: di halaman repo terlihat folder `.github`, `android`, dan `web`.

## Langkah 2: Buat Firebase untuk sinkron (10 menit)

1. Buka **console.firebase.google.com** dan login pakai akun Google.
2. Klik **Create a project** (atau *Buat project*), beri nama `jejak-langkah`. Google Analytics boleh dimatikan. Klik **Create**.
3. Di menu kiri, buka **Build › Firestore Database**, lalu klik **Create database**.
   - Lokasi: pilih **asia-southeast2 (Jakarta)**.
   - Mode: pilih **production mode**, lalu **Create**.
4. Buka tab **Rules**. Hapus semua isinya, tempel teks di bawah, lalu klik **Publish**:

   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /sync/{code}/jejak/{id} {
         allow read, write: if code.size() >= 20;
       }
     }
   }
   ```

5. Klik ikon ⚙️ di kiri atas, lalu **Project settings**. Gulir ke **Your apps** dan klik ikon **`</>`** (Web).
   - Nama app: `jejak`. **Jangan** centang Firebase Hosting. Klik **Register app**.
   - Akan muncul kode berisi `const firebaseConfig = { apiKey: "...", ... }`. **Salin bagian `{ ... }`-nya.**
6. Kembali ke repo GitHub-mu. Buka folder **web**, lalu file **firebase-config.js**, dan klik ikon ✏️ (Edit).
   - Ganti bagian `{ apiKey: "", ... }` dengan yang kamu salin. Biarkan `window.FIREBASE_CONFIG =` di depannya.
   - Klik **Commit changes**.

✅ **Cek**: file `firebase-config.js` sekarang berisi `apiKey: "AIza..."` dan `projectId: "jejak-langkah-..."`.

## Langkah 3: Nyalakan web di GitHub Pages (3 menit)

1. Di repo, buka **Settings › Pages**.
2. Di **Source**, pilih **GitHub Actions**.
3. Buka tab **Actions**, pilih **Web (GitHub Pages)** di kiri, klik **Run workflow**, lalu **Run workflow** lagi.
4. Tunggu sampai ada centang hijau (sekitar 1 menit).

✅ **Cek**: buka `https://NAMA-AKUN-GITHUB.github.io/jejak-langkah/` (ganti NAMA-AKUN-GITHUB, pakai huruf kecil). Jejak Langkah harus muncul.

## Langkah 4: Ambil APK untuk HP (5 menit)

1. Di tab **Actions**, pilih **Android APK**. Kalau belum jalan, klik **Run workflow**. Tunggu centang hijau (sekitar 3–5 menit).
2. Di HP, buka `https://github.com/NAMA-AKUN-GITHUB/jejak-langkah/releases`.
3. Ketuk **JejakLangkah.apk** untuk mengunduh, lalu buka file itu.
4. Kalau muncul peringatan, izinkan **Install dari sumber ini** untuk browser yang kamu pakai, lalu **Install**.
   - Play Protect mungkin bilang "aplikasi tidak dikenal". Itu wajar karena aplikasinya buatan sendiri. Pilih **Install anyway** (Tetap instal).

## Langkah 5: Sambungkan HP dan laptop

**Di HP (perangkat pertama):**
1. Buka **Jejak Langkah**. Akan muncul pop-up "Sambungkan perangkat ini".
2. Tekan **Buat kode baru**, lalu **Salin**. Kirim kodenya ke dirimu sendiri, misalnya lewat WhatsApp ke nomor sendiri.

**Di laptop Windows:**
1. Buka **Microsoft Edge** dan masuk ke `https://NAMA-AKUN-GITHUB.github.io/jejak-langkah/`.
2. Klik **⋯** di kanan atas, lalu **Apps › Install this site as an app** (Aplikasi › Instal situs ini sebagai aplikasi).
3. Sekarang Jejak Langkah ada di Start menu dan bisa di-pin ke taskbar.
4. Di pop-up sinkron, tempel kode dari HP, lalu tekan **Sambungkan**.

✅ **Cek**: tulisan di bawah judul berubah jadi **"Sinkron HP ↔ laptop"** dengan titik hijau. Coba tambah satu langkah di HP. Dalam beberapa detik langkah itu muncul juga di laptop.

## Langkah 6: Pakai bubble

1. Di aplikasi HP, tekan **◉ Tampilkan bubble**.
2. Pertama kali, HP membuka layar izin. Nyalakan **Tampilkan di atas aplikasi lain** (*Display over other apps*), lalu tekan kembali. Bubble langsung muncul.
3. Cara pakai bubble:
   - **Ketuk**: kartu kecil muncul untuk mencatat langkah, menandai ✓/✗, dan pindah cabang.
   - **Tahan**: membuka peta lengkap.
   - **Geser**: memindahkan bubble. Seret ke tanda ✕ di bawah layar untuk mematikannya.
   - Bubble juga bisa dimatikan dari notifikasi: **Matikan bubble**.

Di HP Xiaomi/Oppo/Vivo, kalau bubble hilang sendiri: buka **Setelan › Aplikasi › Jejak Langkah › Baterai**, pilih **Tanpa batasan**.

---

## Kalau ada yang gagal

- **Centang merah di Actions**: klik build yang gagal, salin pesan error-nya (baris merah), lalu kirim ke Claude.
- **APK terbuka tapi layarnya putih atau "Web page not available"**: Langkah 3 belum selesai, atau nama repo bukan `jejak-langkah`. Jalankan ulang **Android APK** setelah web hidup.
- **Tulisan "Sinkron belum diatur"**: `firebase-config.js` belum terisi (Langkah 2.6).
- **"Sinkron terputus"**: cek internet, atau cek lagi Rules di Langkah 2.4.

## Kalau mau mengubah aplikasinya nanti

- Ubah tampilan atau fitur: edit `web/index.html`. Web, APK, dan Windows otomatis ikut berubah tanpa install ulang.
- Ubah bubble atau bagian khusus Android: edit file di folder `android`. GitHub otomatis membuat APK baru di Releases, dan APK baru bisa langsung di-install menimpa yang lama.
