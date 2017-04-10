var express = require('express');
var app = express();
var expressWs = require('express-ws')(app);

var formattedData = {
  locations: [
    {
        device: "A",
        x: 0,
        y: 0,
        z: 1,
    },
    {
        device: "B",
        x: 1,
        y: 0,
        z: 0,
    },
    {
        device: "C",
        x: 0,
        y: 0,
        z: -1,
    },
    {
        device: "D",
        x: -1,
        y: 0,
        z: 0,
    },
    {
        device: "E",
        x: 0,
        y: 0.5,
        z: 0,
    }
  ],
  debug: [
    {
        device: "A",
        field: "RSSI Value",
        value: 12
    },
    {
        device: "C",
        field: "Accelerometer X",
        value: 8
    }
  ]
};

var rawData = {};

function clearData() {
  for (key in rawData) {
    if (Date.now() - rawData[key].lastUpdate >= 5000) {
      delete rawData[key];
    };
  }
};

function addData(info) {
  if rawData.hasOwnProperty(info.device) {
    idx = rawData[info.device].info.findIndex(x => x.field == info.field);
    if (idx == -1) {
      rawData[info.device].info.push(info);
    }
    else {
      rawData[info.device].info[idx] = info
    }
  }
  else {
    rawData[info.device].info.push(info);
  };
  rawData[info.device].lastUpdate = Date.now();
};

function formatData() {
  
}

app.get('/', function(req, res){
  res.send('hello world');
});

app.ws('/socket', function(ws, req) {

  console.log("New connection has opened!");

  //send updated information every 100 ms
  var sendData = setInterval(function(){
    clearData();
    formatData();
    ws.send(JSON.stringify(formattedData));
  }, 100);

  ws.on('close', function() {
      clearInterval(sendData);
      console.log('The connection was closed!');
  });

  ws.on('message', function(msg) {
    console.log(msg);
  });
  //console.log('socket', req);

});

app.listen(process.env.PORT || 3000);
