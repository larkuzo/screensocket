#include "client.h"

#include <iostream>

Client::Client(QWebSocket *socket, QObject *parent) : QObject(parent) {
    this->socket = socket;

    benchmarkTimer = new QTimer(this);
    benchmarkTimer->setInterval(1000);
    frameCount.store(0);

    connect(benchmarkTimer, &QTimer::timeout, this, &Client::countFrame);
    connect(socket, &QWebSocket::disconnected, this, &Client::disconnect);
    connect(socket, &QWebSocket::readChannelFinished, this, &Client::disconnect);
    connect(socket, &QWebSocket::textMessageReceived, [=](const QString &message) {
        if (message == "OK") ready = true;
    });

    benchmarkTimer->start();
}

bool Client::send(const QByteArray &data) {
    if (ready) {
        auto sent = socket->sendBinaryMessage(data);
        ready = false;

        if (sent == data.size()) {
            ++frameCount;
            totalData += data.size();
            return true;
        } else {
            return false;
        }
    }

    // frame skipped, so don't delete connection
    return true;
}

void Client::disconnect() {
    if (!disconnected) {
        disconnected = true;
        ready = true;
        benchmarkTimer->stop();
        socket->close();
        socket->deleteLater();
    }
}

void Client::countFrame() {
    std::cout << time << "," << totalData << std::endl;
    //    std::cout << time << "," << frameCount << std::endl;
    frameCount.store(0);
    totalData.store(0);
    ++time;
}
