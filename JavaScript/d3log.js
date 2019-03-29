import * as d3 from "d3"

export function plot(selector, allCurves, data) {
    let indexCurve = allCurves[0]
    let curves = allCurves.slice(1)
  
    let indexFormat = null
    if (indexCurve.valueType == 'datetime') {
      data.forEach(r => r[0] = d3.isoParse(r[0]))
      indexFormat = d3.timeFormat('%H:%M:%S')
    }
  
    let height = 2000;
    let trackWidth = 180;
    let headerHeight = 34;
  
    let colors = d3.scaleOrdinal(d3.schemeCategory10)
    let tracks = d3.scaleBand()
      .padding(.25)
      .paddingOuter(0.33)
      .range([0, trackWidth * curves.length])
      .domain(d3.range(curves.length))
    let indexScale = d3.scaleLinear()
      .range([0, height])
      .domain(d3.extent(data, r => r[0]))
    let indexAxis = d3.axisRight(indexScale)
      .ticks(20).tickSize(tracks.bandwidth()).tickFormat('')
  
    selector.selectAll('*').remove()
    let plot = selector.append('svg')
      .attr('width', trackWidth * curves.length)
      .attr('height', height)
  
    // Create gradients for each curve
    let gradient = plot.append('defs').selectAll('linearGradient')
      .data(curves)
      .enter().append('linearGradient')
        .attr('id', (d, i) => `gradient${i}`)
    gradient.append('stop').attr('offset', 0).style('stop-color', (d,i) => colors(i)).style('stop-opacity', 0.0)
    gradient.append('stop').attr('offset', 1).style('stop-color', (d,i) => colors(i)).style('stop-opacity', 0.75)
  
    // Create groups for each curve
    let gr = plot.selectAll('g')
      .data(curves)
      .enter().append('g')
        .attr('transform', (d,i) => `translate(${tracks(i)},${headerHeight})`)
  
    let line = d3.local()
    let area = d3.local()
    let axis = d3.local()
  
    // Set up individual scaling, axis and line/area functions in d3.local variables
    gr.each(function(d, i) {
      let c = i + 1 // curve column
  
      let curveScale = d3.scaleLinear()
        .range([0, tracks.bandwidth()])
        .domain(d3.extent(data, r => r[c]))
        .nice(5)
      let domain = curveScale.domain()
  
      axis.set(this, d3.axisTop(curveScale).ticks(5).tickSize(-height))
      line.set(this, d3.line()
        .defined(r => r[0] != null && r[c] != null)
        .y(r => indexScale(r[0]))
        .x(r => curveScale(r[c])))
      let baseline = domain[0] < 0 && domain[1] > 0 ? 0.0 : domain[0]
      area.set(this, d3.area()
        .defined(r => r[0] != null && r[c] != null)
        .y(r => indexScale(r[0]))
        .x0(curveScale(baseline))
        .x1(r => curveScale(r[c])))
    })
    // Based on Mike Bostock: Local Variables (https://bl.ocks.org/mbostock/e1192fe405703d8321a5187350910e08)/

  
    // Draw all the track elements
    gr.append('rect')
      .attr('class', 'back')
      .attr('width', tracks.bandwidth())
      .attr('height', height)
  
    gr.append('text')
      .attr('transform', `translate(0,-22)`)
      .attr('text-anchor', 'middle')
      .attr('x', tracks.bandwidth() / 2)
      .text(d => `${d.name} [${d.unit}]`)
  
    gr.append('g')
      .attr('class', 'axis')
      .each(function(d) { axis.get(this)(d3.select(this)) })
  
    gr.append('g')
      .attr('class', 'axis')
      .each(function(d) { indexAxis(d3.select(this)) })
  
    gr.append('path')
      .attr('class', 'area')
      .style('fill', (d, i) => `url(#gradient${i})`)
      .attr('d', function(d) { return area.get(this)(data) })
  
    gr.append('path')
      .attr('class', 'line')
      .style('stroke', (d, i) => colors(i))
      .attr('d', function(d) { return line.get(this)(data) })
  
    // Add index axis on left and right side:
    plot.append('g')
      .attr('transform', `translate(${tracks(0)},${headerHeight})`)
      .attr('class', 'axis')
      .call(d3.axisLeft(indexScale).ticks(20).tickSize(6).tickFormat(indexFormat))
  
    plot.append('g')
      .attr('transform', `translate(${tracks(curves.length-1) + tracks.bandwidth()},${headerHeight})`)
      .attr('class', 'axis')
      .call(d3.axisRight(indexScale).ticks(20).tickSize(6).tickFormat(indexFormat))
}