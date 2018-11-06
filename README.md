# ALUR PROGRAM

## Alur discovery server :
1. Server membuka UDP socket dan WebSocket dengan port tertentu
2. Client membuat UDP socket dan melakukan broadcast UDP packet ke port tertentu dengan data berupa IP address dan port UDP socket client
3. Server menerima UDP socket dan mengirim IP address dan port WebSocket server
4. Client menerima IP address dan port WebSocket server
5. Client menghubungkan WebSocket ke server

## Alur transaksi frame :
1. Server mengambil 1 frame tampilan layar
2. Server melakukan kompresi frame menggunakan format JPG dengan kualitas tertentu
3. Server memeriksa apakah client siap (ready) menerima frame baru
	- Jika iya, kirim ke client
	- Jika tidak, skip client untuk frame tersebut (drop frame)
4. Server mengubah status client menjadi not ready
5. Client menerima frame
6. Client melakukan decode frame (dan memunculkan di layar pada update selanjutnya)
7. Client mengirim pesan "OK" untuk menandakan bahwa client siap menerima frame baru
8. Server menerima pesan "OK" dari client
9. Server mengubah status client menjadi ready
10. Ulangi dari 1, hal ini dilakukan terus-menerus dengan framerate tertentu
