var locations = [
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
]

var debug = [
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
];

var colorMappings = {
    A: "rgb(255, 99, 132)",
    B: "rgb(54, 162, 235)",
    C: "rgb(255, 206, 86)",
    D: "rgb(75, 192, 192)",
    E: "rgb(153, 102, 255)"
}

var locationCharts = {
    xy: null,
    xz: null,
    yz: null
}
var debugCharts = {};

function saveCanvas() {
    var image = this.toDataURL("image/png");
    image = image.replace(/^data:image\/[^;]*/, 'data:application/octet-stream');
    image = image.replace(/^data:application\/octet-stream/, 'data:application/octet-stream;headers=Content-Disposition%3A%20attachment%3B%20filename=Canvas.png');

    window.location.href = image;
}

function makeLocationDataset(locations, x, y) {
    return locations.map(function(device) {
        return {
            label: device.device,
            data: [{
                x: device[x],
                y: device[y],
                r: 6
            }],
            backgroundColor: colorMappings[device.device],
        };
    });
}

function makeLocationChart(plane, title) {
    canvas = document.getElementById(plane + "_plane");
    canvas.onclick = saveCanvas;
    locationCharts[plane] = new Chart(canvas, {
        type: 'bubble',
        data: {
            datasets: []
        },
        options: {
            title: {
                display: true,
                text: title
            },
            animation: false
        }
    });
}

function makeLocationCharts(locations) {
    if (locationCharts.xy == null) {
        makeLocationChart("xy", "Front View");
    }
    if (locationCharts.xz == null) {
        makeLocationChart("xz", "Top View");
    }
    if (locationCharts.yz == null) {
        makeLocationChart("yz", "Side View");
    }

    locationCharts.xy.data.datasets = makeLocationDataset(locations, "x", "y");
    locationCharts.xy.update();

    locationCharts.xz.data.datasets = makeLocationDataset(locations, "x", "z");
    locationCharts.xz.update();

    locationCharts.yz.data.datasets = makeLocationDataset(locations, "z", "y");
    locationCharts.yz.update();
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

function makeDebugCharts(debug) {
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
    var ws = new WebSocket("ws://localhost:9998/echo");

    ws.onopen = function() {
     // Web Socket is connected, send data using send()
     ws.send("Message to send");
     alert("Message is sent...");
    };

    ws.onmessage = function (evt) {
     var received_msg = evt.data;
     alert("Message is received...");
    };

    ws.onclose = function() {
     // websocket is closed.
     alert("Connection is closed...");
    };
}

function noise() {
    debug[0].value += Math.random() - 0.55;
    debug[1].value += Math.random() - 0.49;
    locations[4].x += Math.random() * 0.1 - 0.05;
    locations[4].y += Math.random() * 0.1 - 0.05;
    locations[4].z += Math.random() * 0.1 - 0.05;
}

window.onload = function() {
    Chart.defaults.global.legend.display = false;
    Chart.defaults.global.tooltips.enabled = false;

    setUpWebSocket();

    setInterval(function() {
        noise();
        makeLocationCharts(locations);
        makeDebugCharts(debug);
    }, 100);
}
