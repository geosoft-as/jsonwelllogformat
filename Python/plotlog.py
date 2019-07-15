import matplotlib.pyplot as plt
import json

with open("realtime.json", "r") as file_handle:
    file = json.load(file_handle)

curves = file[0]['curves']
data = file[0]['data']

index = list(map(lambda row: row[0], data))

for curveIndex in range(1, len(curves)):
    curve = list(map(lambda row: row[curveIndex], data))
    plt.subplot(1, len(curves), curveIndex)
    plt.plot(curve, index)

    ax = plt.gca()
    ax.invert_yaxis()
    ax.xaxis.tick_top()
    ax.xaxis.set_label_position('top')
    if curveIndex > 1: ax.set_yticklabels([])

    if curveIndex == 1: 
        plt.ylabel(f'{curves[0]["name"]} [{curves[0]["unit"]}]')
    plt.xlabel(f'{curves[curveIndex]["name"]} [{curves[curveIndex]["unit"]}]')

plt.gcf().suptitle("JSON Well Log")
plt.show()