#include "client.h"

#include <iostream>

Client::Client(QWebSocket *socket, QObject *parent) : QObject(parent) {
    this->socket = socket;

    connect(socket, &QWebSocket::disconnected, [=]() { this->disconnect(); });
    connect(socket, &QWebSocket::textMessageReceived, [=](const QString &message) {
        if (message == "OK") ready = true;
    });
}

bool Client::send(const QByteArray &data) {
    if (ready) {
        auto sent = socket->sendBinaryMessage(data);
        ready = false;
        return sent == data.size();
    }

    // frame skipped, so don't delete connection
    qDebug() << "frame skip";
    return true;
}

void Client::disconnect() {
    socket->close();
    socket->deleteLater();
}
