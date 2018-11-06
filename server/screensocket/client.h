#ifndef CLIENT_H
#define CLIENT_H

#include <QtWebSockets>

class Client : QObject {
    Q_OBJECT

private:
    QWebSocket *socket;
    volatile bool ready = true;

public:
    Client(QWebSocket *socket, QObject *parent = nullptr);
    bool send(const QByteArray &data);
    void disconnect();
};

#endif // CLIENT_H
