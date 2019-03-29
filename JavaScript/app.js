import Vue from 'vue'
import * as d3 from 'd3'
import * as d3log from './d3log'

// Removes cross origin scripting checks. Do not use in production!
let proxyToBypassSecurity = 'https://cors-anywhere.herokuapp.com/'
proxyToBypassSecurity = ''

let states = [ 'include', 'filter', 'exclude' ]

const wrapFilter = text => ({ text, state: states[0] })
let data = require('./toc.json')

let filters = [
    { key: 'type', values: data.summary.type.map(wrapFilter) },
    { key: 'well', values: data.summary.well.map(wrapFilter) },
    { key: 'operator', values: data.summary.operator.map(wrapFilter) },
    { key: 'serviceCompany', values: data.summary.serviceCompany.map(wrapFilter) },
    { key: 'date', values: data.summary.date.map(wrapFilter) }
]
let filterColors = d3.scaleOrdinal(d3.schemeCategory10)
filters.forEach((filter, index) => filter.style = { 
    '--filter-background': filterColors(index), 
    '--filter-hover-background': d3.color(filterColors(index)).darker(0.5).hex() 
})

function computeFilter(include, filter, exclude) {
    if (filter.length > 0) { return filter }
    return include.filter(item => !exclude.includes(item))
}

let app = new Vue({
    el: '#app',
    data: {
        filters,
        items: data.items,
        loading: false
    },
    computed: {
        filterCache: function() { 
            return filters.map(filter => ({ key: filter.key, values: computeFilter(
                filter.values.filter(i => i.state == states[0]).map(i => i.text),
                filter.values.filter(i => i.state == states[1]).map(i => i.text),
                filter.values.filter(i => i.state == states[2]).map(i => i.text)
            )}))
        }
    },
    methods: {
        toggle: function(item) { 
            item.state = states[(states.indexOf(item.state) + 1) % states.length] 
        },
        filterMatch: function(item) { 
            return this.filterCache
                .map(filter => filter.values.includes(item[filter.key]) || !(item[filter.key]))
                .reduce((prev, curr) => prev && curr, true)
        },
        load: function(file) {
            this.loading = true
            fetch(proxyToBypassSecurity + file.url, { headers: { 'X-Requested-With': 'fetch' }})
            .then(response => response.json())
            .then(files => {
                let file = files[0]
                d3log.plot(d3.select('#plot'), file.curves, file.data)
            })
            .then(() => {
                this.loading = false
            })
        }
    }
})


