var express = require('express');
var app = express();
var expressWs = require('express-ws')(app);

app.use(express.static("html"));

var formattedData = {};
var rawData = {};

function clearData() {
  for (key in rawData) {
    if (Date.now() - rawData[key].lastUpdate >= 300000) {
      delete rawData[key];
    };
  }
};

function findField(arr, key = 'field', val) {
  for (i=0; i<arr.length; i++) {
    if (arr[i][key] == val) {
      return i;
    }
    return -1;
  }
}

function addData(info) {
  if (rawData.hasOwnProperty(info.device)) {
    var idx = findField(rawData[info.device].info, info.field);
    if (idx == -1) {
      rawData[info.device].info.push(info);
    }
    else {
      rawData[info.device].info[idx] = info
    }
  }
  else {
    rawData[info.device] = {};
    rawData[info.device].info = [info];
  };
  rawData[info.device].lastUpdate = Date.now();
};

function formatData() {
  formattedData = {locations: [], debug: []};
  for (device in rawData) {
    for (i in rawData[device].info) {
      var prop = rawData[device].info[i];
      if (prop.field == 'Location') {
        item = Object.assign({}, prop);
        delete item.field;
        formattedData.locations.push(item);
      }
      else {
        formattedData.debug.push(prop);
      };
    };
  };
  console.log(formattedData);
};

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

app.ws('/messages', function(ws) {
  ws.on('message', function(msg) {
    addData(JSON.parse(msg));
    console.log(JSON.stringify(rawData));
  });
});

app.listen(process.env.PORT || 3000);
