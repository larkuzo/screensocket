#include "mainwindow.h"

#include <iostream>

#define FPS 24
#define QUALITY 60

QByteArray stripAlpha(const QImage &image) {
    // Convert BGRA to RGB
    auto data = QByteArray(static_cast<int>(image.sizeInBytes() / 4 * 3), 0);
    auto array = image.bits();
    for (int i = 0; i < image.sizeInBytes() / 4; ++i) {
        auto b = array[i * 4 + 0];
        auto g = array[i * 4 + 1];
        auto r = array[i * 4 + 2];

        data[i * 3 + 0] = static_cast<char>(r);
        data[i * 3 + 1] = static_cast<char>(g);
        data[i * 3 + 2] = static_cast<char>(b);
    }

    return data;
}

MainWindow::MainWindow(QWidget *parent) : QMainWindow(parent) {
    server = new QWebSocketServer("screensocket", QWebSocketServer::NonSecureMode, this);
    socket = new QUdpSocket(this);

    screen = QApplication::primaryScreen()->availableGeometry();
    dimension = QString::number(screen.width()) + "x" + QString::number(screen.height());

    timer = new QTimer(this);
    timer->setInterval(1000 / FPS);

    preview = new QLabel;
    preview->setAlignment(Qt::AlignCenter);

    auto wrapper = new QWidget;
    auto layout = new QVBoxLayout;
    auto buttonLayout = new QHBoxLayout;
    startButton = new QPushButton("Start");
    stopButton = new QPushButton("Stop");

    buttonLayout->addWidget(startButton);
    buttonLayout->addWidget(stopButton);
    layout->addLayout(buttonLayout);
    layout->addWidget(preview);

    startButton->setEnabled(true);
    stopButton->setEnabled(false);

    connect(startButton, &QPushButton::clicked, this, &MainWindow::startCapture);
    connect(stopButton, &QPushButton::clicked, this, &MainWindow::stopCapture);
    connect(timer, &QTimer::timeout, this, &MainWindow::captureScreen);
    connect(socket, &QAbstractSocket::readyRead, this, &MainWindow::broadcastReceived);
    connect(server, &QWebSocketServer::newConnection, this, &MainWindow::connectionReceived);

    wrapper->setLayout(layout);
    setCentralWidget(wrapper);
}

void MainWindow::startCapture() {
    server->listen(QHostAddress::Any, 7331);
    socket->bind(QHostAddress::Any, 7332);
    timer->start();

    auto address = server->serverAddress().toString().toUtf8().constData();
    auto port = server->serverPort();
    std::cout << "Server started at " << address << ":" << port << std::endl;

    startButton->setEnabled(false);
    stopButton->setEnabled(true);
}

void MainWindow::stopCapture() {
    timer->stop();
    socket->close();

    clientsMutex.lock();
    for (int i = 0; i < clients.size(); ++i) {
        clients[i]->disconnect();
    }

    clients.clear();
    clientsMutex.unlock();

    server->close();

    std::cout << "Server stopped" << std::endl;

    startButton->setEnabled(true);
    stopButton->setEnabled(false);
}

void MainWindow::captureScreen() {
    QPixmap buffer = QApplication::primaryScreen()->grabWindow(0);

    if (clients.size() > 0) this->sendImage(buffer.toImage());
    preview->setPixmap(buffer.scaled(preview->size(), Qt::KeepAspectRatio, Qt::SmoothTransformation));
}

void MainWindow::broadcastReceived() {
    while (socket->hasPendingDatagrams()) {
        QNetworkDatagram datagram = socket->receiveDatagram();
        std::cout << "New datagram" << std::endl;

        foreach (const QHostAddress &address, QNetworkInterface::allAddresses()) {
            auto isIPv4 = address.protocol() == QAbstractSocket::IPv4Protocol;
            if (!address.isBroadcast() && !address.isLoopback() && isIPv4) {
                QByteArray packet = (address.toString() + ":7331").toUtf8();
                QNetworkDatagram data(packet, datagram.senderAddress(), static_cast<quint16>(datagram.senderPort()));
                socket->writeDatagram(data);
                break;
            }
        }
    }
}

void MainWindow::connectionReceived() {
    QWebSocket *socket = server->nextPendingConnection();
    socket->sendTextMessage(dimension);

    clientsMutex.lock();
    clients.push_back(new Client(socket, this));
    clientsMutex.unlock();

    std::cout << "New client connected." << std::endl;
}

void MainWindow::sendImage(const QImage &image) {
    // broadcastData(stripAlpha(image));
    // return;

    QByteArray data;
    QBuffer buffer(&data);
    buffer.open(QIODevice::WriteOnly);
    image.save(&buffer, "JPG", QUALITY);

    qDebug() << "size: " << data.size();
    //    std::cout << "size: " << data.size() << std::endl;
    broadcastData(data);
}

void MainWindow::broadcastData(const QByteArray &data) {
    clientsMutex.lock();

    QVector<int> disconnected;
    for (int i = 0; i < clients.size(); ++i) {
        auto ok = clients[i]->send(data);
        if (!ok) disconnected.push_back(i);
    }

    // Clean up disconnected clients
    for (int i = disconnected.size() - 1; i >= 0; --i) {
        clients[disconnected[i]]->disconnect();
        clients.remove(disconnected[i]);
    }

    if (disconnected.size() > 0) {
        std::cout << disconnected.size() << " client(s) disconnected" << std::endl;
    }

    clientsMutex.unlock();
}
