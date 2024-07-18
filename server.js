const WebSocket = require('ws');
const fs = require('fs');
const wss = new WebSocket.Server({ host: '0.0.0.0', port: 1029 });

wss.on('connection', function connection(ws) {
  ws.on('message', function message(data) {
    console.log('sensor received: %s', data);
    fs.appendFile('sensor.json', data + '\n', (err) => {
      if (err) {
        console.error('Error writing to file:', err);
      }
    });
  });
});