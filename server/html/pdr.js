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

var debugChart = null;
var lastPoint = {x: 0, y: 0};

function saveCanvas() {
    var image = this.toDataURL("image/png");
    image = image.replace(/^data:image\/[^;]*/, 'data:application/octet-stream');
    image = image.replace(/^data:application\/octet-stream/, 'data:application/octet-stream;headers=Content-Disposition%3A%20attachment%3B%20filename=Canvas.png');

    window.location.href = image;
}

function makeChart() {
    var canvas = document.createElement("canvas");
    document.getElementById("location").append(canvas);
    canvas.onclick = saveCanvas;

    points = [];
    for (var i = 100; i >= 0; i--) {
        points.push({x: 0, y: 0});
    }
    var data = {
        datasets: [
            {
                fill: false,
                lineTension: 0.1,
                borderColor: colorMappings['B'],
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
                data: points,
                spanGaps: true,
            }
        ]
    };

    debugChart = {
        chart: new Chart(canvas, {
            type: 'scatter',
            data: data,
            options: {
                animationSteps: 15,
                title: {
                    display: true,
                    text: "Pedestrian Dead Reckoning"
                }
            }
        }),
        data: data
    };
}

function updateChart(point) {
    var data = debugChart.data.datasets[0].data;
    data.shift();
    data.push(point);
    debugChart.chart.update();
}

function setUpWebSocket() {
    var ws = new WebSocket("wss://microlocation.herokuapp.com/socket");

    ws.onopen = function() {
        console.log("WebSocket Connected");
    };

    ws.onmessage = function (evt) {
        var data = JSON.parse(evt.data);
        steps = null;
        x = null;
        z = null;
        for(var i = 0; i < data.debug.length; i++) {
            entry = data.debug[i];
            if(entry.device == "B") {
                if(entry.field == "X") {
                    x = entry.value;
                } else if(entry.field == "Z") {
                    z = entry.value;
                } else if(entry.field == "Step") {
                    steps = entry.value;
                }
            }
        }
        if (x != null && z != null && steps != null) {
            document.getElementById("steps").innerText = steps;
            if (x != lastPoint.x || z != lastPoint.y) {
                lastPoint = {x: x, y: z}
                updateChart(lastPoint);
            }
            console.log(x, z);
        }
    };

    ws.onclose = function() {
        console.log("WebSocket Disconnected");
    };
}

window.onload = function() {
    Chart.defaults.global.legend.display = false;
    Chart.defaults.global.tooltips.enabled = false;

    makeChart();
    setUpWebSocket();
}
