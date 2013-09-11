function drawDemograph() {

    var data = [1, 3, 2];

    // Bar Chart Tutorial pt 3
    var x3 = d3.scale.linear()
        .domain([0, d3.max(queryData)])
        .range(["0px","2000px"]);
    var y3 = d3.scale.ordinal()
        .domain(queryData)
        .rangeBands([0, 20 * queryData.length]);
    var chart3 = d3.select("#test_graph").append("svg")
        .attr("class", "chart")
        .attr("width", 420)
        .attr("height", 20 * queryData.length);

    chart3.selectAll("rect")
        .data(queryData)
        .enter().append("rect")
        .attr("y", function(d, i) { return i * y3.rangeBand(); })
        .attr("width", x3)
        .attr("height", y3.rangeBand());

}
