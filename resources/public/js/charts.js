var barGap = 5;
var datePadding = 10;

/* 
   Other charts may be added here in the same way:
   <function defining chart space>
   <rendering function>
*/

function checkinsDemonstration (location, dimension, lowDay, highDay, group1, group2) {

    return dc.barChart(location)
        .height(450)
        .width(900)
        .gap(barGap)
        .centerBar(true)
        .dimension(dimension)
        .group(group1)
        .xUnits(d3.time.days) // function calculating bar width
        .renderHorizontalGridLines(true)
        .elasticX(true)
        .xAxisPadding(datePadding)
        .stack(group2)
        .x(d3.time.scale().domain([ lowDay, highDay ]))
        .brushOn(false)
        .margins({top: 20, bottom: 20, right: 30, left: 30})
        .xAxis().tickFormat(d3.time.format("%Y-%m-%d"));
}



function drawCheckins() {

// Format data for display

console.log(queryData);

var filtered = crossfilter(queryData);

var day = filtered.dimension( function (f) { return new Date(f.date);} );

var highestDay = new Date( day.top(1)[0].date );
var lowestDay = new Date( day.bottom(1)[0].date );

var puppetdb_by_day = day.group(d3.time.day).reduceSum( function(p) { if ("puppetdb" == p.product) {return p.count;} else return 0; });

var pemaster_by_day = day.group(d3.time.day).reduceSum( function(p) { if ("pe-master" == p.product) {return p.count;} else return 0; });

// Draw data

checkinsDemonstration( "#checkins_by_date_bars", day, lowestDay, highestDay, puppetdb_by_day, pemaster_by_day );

checkinsLineGraph( "#checkins_by_date_lines", day, lowestDay, highestDay, puppetdb_by_day, pemaster_by_day );

dc.renderAll();

}
