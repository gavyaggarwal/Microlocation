var colorMappings = {
    A: "rgb(255, 99, 132)",
    B: "rgb(54, 162, 235)",
    C: "rgb(255, 206, 86)",
    D: "rgb(75, 192, 192)",
    E: "rgb(153, 102, 255)"
};

var colorMappingsHex = {
    A: "#FF6384",
    B: "#36A2EB",
    C: "#FFCE56",
    D: "#4BC0C0",
    E: "#9866FF"
};

var locationChart= null;
var debugCharts = {};

function saveCanvas() {
    var image = this.toDataURL("image/png");
    image = image.replace(/^data:image\/[^;]*/, 'data:application/octet-stream');
    image = image.replace(/^data:application\/octet-stream/, 'data:application/octet-stream;headers=Content-Disposition%3A%20attachment%3B%20filename=Canvas.png');

    window.location.href = image;
}

function updateLocationCharts(locations) {
    var data = locations.map(function(device) {
        return {
            x: device.x,
            y: device.y,
            z: device.z,
            style: colorMappingsHex[device.device]
        };
    });

    if (locationChart == null) {
        var options = {
          width:  'calc(100vw - 17px)',
          height: '80vh',
          showShadow: true,
          verticalRatio: 0.5,
          cameraPosition: {horizontal: 0, vertical: 1.57, distance: 2.0},
          style: 'dot-color',
          showLegend: false,
          xMax: 2,
          xMin: -2,
          yMax: 2,
          yMin: -2,
          zMax: 2,
          zMin: -2,
        };

        var container = document.getElementById('locations');
        locationChart = new vis.Graph3d(container, data, options);
    } else {
        locationChart.setData(data);
        locationChart.redraw();
    }
}

function makeDebugChart(name, deviceName) {
    var canvas = document.createElement("canvas");
    document.getElementById("debug").append(canvas);
    canvas.onclick = saveCanvas;

    var labels = [];
    var values = []
    for (var i = 100; i >= 0; i--) {
        labels.push(i / parseFloat(10));
        values.push(0);
    }
    var data = {
        labels: labels,
        datasets: [
            {
                fill: false,
                lineTension: 0.1,
                borderColor: colorMappings[deviceName],
                borderCapStyle: 'butt',
                borderDash: [],
                borderDashOffset: 0.0,
                borderJoinStyle: 'miter',
                pointBackgroundColor: "#fff",
                pointBorderWidth: 1,
                pointHoverRadius: 5,
                pointHoverBackgroundColor: "rgba(75,192,192,1)",
                pointHoverBorderWidth: 2,
                pointRadius: 1,
                pointHitRadius: 10,
                data: values,
                spanGaps: true,
            }
        ]
    };

    debugCharts[name] = {
        chart: new Chart(canvas, {
            type: 'line',
            data: data,
            options: {
                animationSteps: 15,
                title: {
                    display: true,
                    text: name
                },
                scales: {
                  xAxes: [{
                      ticks: {
                          autoSkip: true,
                          maxTicksLimit: 11
                      },
                      scaleLabel: {
                        display: true,
                        labelString: 'Seconds Ago'
                      }
                  }]
                }
            }
        }),
        data: data
    };
}

function updateDebugCharts(debug) {
    for (var i = 0; i < debug.length; i++) {
        var name = "Device " + debug[i].device + ": " + debug[i].field;
        if (!(name in debugCharts)) {
            makeDebugChart(name, debug[i].device);
        }
        var data = debugCharts[name].data.datasets[0].data;
        data.shift();
        data.push(debug[i].value);
        debugCharts[name].chart.update();
    }
}

function setUpWebSocket() {
    var ws = new WebSocket("wss://microlocation.herokuapp.com/socket");

    ws.onopen = function() {
        console.log("WebSocket Connected");
    };

    ws.onmessage = function (evt) {
        var data = JSON.parse(evt.data);
        updateLocationCharts(data.locations);
        updateDebugCharts(data.debug);
    };

    ws.onclose = function() {
        console.log("WebSocket Disconnected");
    };
}

window.onload = function() {
    Chart.defaults.global.legend.display = false;
    Chart.defaults.global.tooltips.enabled = false;

    setUpWebSocket();
}
