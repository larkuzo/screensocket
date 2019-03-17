#ifndef CLIENT_H
#define CLIENT_H

#include <QAtomicInt>
#include <QTimer>
#include <QtWebSockets>

class Client : public QObject {
    Q_OBJECT

private:
    QWebSocket *socket;
    volatile bool ready = true;
    bool disconnected = false;
    volatile int time = 0;

    QTimer *benchmarkTimer;
    QAtomicInt frameCount;
    QAtomicInt totalData;

public:
    Client(QWebSocket *socket, QObject *parent = nullptr);
    bool send(const QByteArray &data);

public slots:
    void disconnect();

private slots:
    void countFrame();
};

#endif // CLIENT_H
