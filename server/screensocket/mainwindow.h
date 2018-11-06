#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QUdpSocket>
#include <QtConcurrent>
#include <QtWebSockets>
#include <QtWidgets>
#include <client.h>

class MainWindow : public QMainWindow {
    Q_OBJECT

private:
    QRect screen;
    QString dimension;
    QWebSocketServer *server;
    QUdpSocket *socket;
    QLabel *preview;
    QImage last;
    QTimer *timer;
    QVector<Client *> clients;
    QMutex clientsMutex;

private:
    void sendImage(const QImage &image);
    void broadcastData(const QByteArray &data);

private slots:
    void startCapture();
    void stopCapture();
    void captureScreen();
    void broadcastReceived();
    void connectionReceived();

public:
    MainWindow(QWidget *parent = nullptr);
};

#endif // MAINWINDOW_H
