const fs = require('fs')
const path = require('path')

const skip = [
    '/Users/hallgrim/Downloads/Well_logs/02.LWD_EWL/15_9-F-11 A/WL_RAW_AAC_MWD_2.json',
    '/Users/hallgrim/Downloads/Well_logs-2/02.LWD_EWL/15_9-F-11 A/WL_RAW_AAC_MWD_2.json'
]
const baseurl = 'http://jsonwelllogformat.org/Volve/Well_logs/'
const directory = '/Users/hallgrim/Downloads/Well_logs-2'

function find(start, ext, callback) {
    fs.readdirSync(start).forEach(file => {
        if (fs.statSync(start + '/' + file).isDirectory()) {
            find(start + '/' + file, ext, callback)
        }
        if (file.endsWith(ext)) {
            callback(start + '/' + file)
        }
    })
}

function extract(file, callback) {
    let raw = fs.readFileSync(file)
    let logs = []
    try {
        logs = JSON.parse(raw)
    } catch (e) {
        let pos = parseInt(e.message.split(/\s/).slice(-1)[0])

        let msg = e.message + ' ' +
        raw.slice(pos-40, pos).toString().replace(/\s/g, ' ') + 
        raw.slice(pos, pos+40).toString().replace(/\s/g, ' ')
        console.error(file + ': ' + msg)
    }
    logs.forEach(log => callback({ 
        file: file.replace(directory, ''), 
        header: log.header, 
        curves: log.curves
    }))
}

var operators = {
    "STATOIL": "Equinor",
    "STATOIL ASA": "Equinor",
    "STATOIL PETROLEUM AS": "Equinor",
    "STATOIL PETRPOLEUM AS": "Equinor",
    "STATOILHYDRO": "Equinor",
    "STATOILHYDRO ASA": "Equinor",
    "Statoil": "Equinor",
    "Statoil ASA": "Equinor",
    "Statoil Petroleum": "Equinor",
    "Statoil Petroleum AS": "Equinor",
    "StatoilHydro": "Equinor",
    "StatoilHydro ASA": "Equinor"
}

var serviceCompanies = {
    "SCHLUMBERGER": "Schlumberger",
    "SLB": "Schlumberger",
    "SLB D&M": "Schlumberger",
    "Schlumberger D&M": "Schlumberger",
    "Sclumberger": "Schlumberger",

    "RESLAB A/S": "Weatherford",
    "CORETEAM A/S": "Coreteam",

    "READ WELL SERVICES": "Read",
    "READ Well Services": "Read",

    "CROCKER DATA PROCESSING": "Crocker",
    "FLUID INCLUSION TECH": "Fluid Inclusion",
    "GEOSERVICES": "Schlumberger",
    "Geoservices": "Schlumberger",
    "Halliburton Logging Services": "Halliburton",

    "INTEQ": "Baker Hughes",
    "Baker Hughes INTEQ": "Baker Hughes",
    "BAKER HUGHES INTEQ": "Baker Hughes",
    "150": "Baker Hughes",

    "UNKNOWN": "Unknown"
}

var filters = {
    serviceCompany: v => serviceCompanies[v] || v,
    operator: v => operators[v] || v,
    date: v => {
        let year = (new Date(v)).getFullYear()
        if (year) {
            return year > 1900 ? year : year + 2000 
        } else {
            return "Unknown"
        }
    },
    well: well => (well || 'Unknown well').replace('NO_', '').replace('_', ' ').replace(',', ' ').toUpperCase()
}

function collect(item, sum) {
    Object.keys(item).forEach(key => {
        sum[key] = sum[key] || new Set()
        let value = item[key]
        let filter = filters[key] || (v => v)
        sum[key].add(filter(value))
    })
}

function convert(item, type) {
    return {
        type,
        name: item.header.name,
        well: filters.well(item.header.well),
        operator: filters.operator(item.header.operator || 'Unknown'),
        serviceCompany: filters.serviceCompany(item.header.serviceCompany || 'Unknown'),
        date: filters.date(item.header.date || ''),
        url: baseurl + item.file        
    }
}

function proc(log, list, summary) {
    let type = log.file.split(/\//)[1].substring(3).trim()
    collect(log.header, summary)
    summary.type.add(type)
    list.push(convert(log, type))
}

let files = []
find(directory, 'json', f => files.push(f))
files = files.filter(f => !skip.includes(f))
//files = files.slice(100, 102)

let logs = []
files.forEach(f => extract(f, l => logs.push(l)))

let list = []
var summary = {
    type: new Set(),
    date: new Set([ 'Unknown'] )
}
logs.forEach(l => proc(l, list, summary))

let toc = {
    summary,
    items: list
}

Object.keys(summary).forEach(key => summary[key] = [...summary[key]].sort())

delete summary.name
delete summary.startIndex
delete summary.endIndex
delete summary.step
delete summary.country
delete summary.field
delete summary.runNumber
delete summary.source

fs.writeFileSync('toc.json', JSON.stringify(toc, null, 4))