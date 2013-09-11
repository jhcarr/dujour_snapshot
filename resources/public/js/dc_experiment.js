function checkinsDemonstration (location, dimension, group) {

    console.log("Begin drawCheckinsDemonstration");

//    var dimensionGroup = dimension.group().reduceCount().orderNatural();

    return dc.barChart(location)
        .height(450)
        .width(900)
        .dimension(dimension)
        .group(group)
        .x(d3.time.scale().domain([new Date(2013, 6, 1), new Date(2013, 6, 20)]))
        .margins({top: 50, bottom: 50, right: 50, left: 50})
        .transitionDuration(0)
        .xAxis().tickFormat(d3.time.format("%Y-%m-%d"));
}

function draw() {

console.log(queryData);

var filtered = queryData.map( function(d){
    return {
        'count': d.count,
        'product': d.product + ' ' + d.version,
        'timestamp': new Date(d.timestamp),
        'ip': d.ip
    };
});

filtered = crossfilter(filtered);

var timestamp = filtered.dimension( function (f) { return f.timestamp;} );

var timestamps = timestamp.group(d3.time.minute).reduceCount();

checkinsDemonstration( '#dc-chart', timestamp, timestamps );

dc.renderAll();


}
