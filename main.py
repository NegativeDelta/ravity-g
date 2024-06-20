from os import listdir
from time import sleep
import matplotlib.pyplot as plt
import csv

zapisany_stan_folderu = listdir("C:\\xampp\\htdocs")
print(zapisany_stan_folderu)

while True:
    if listdir("C:\\xampp\\htdocs") != zapisany_stan_folderu:
        filename = list(set(listdir("C:\\xampp\\htdocs")) - set(zapisany_stan_folderu))[0]
        print(filename)
        zapisany_stan_folderu = listdir("C:\\xampp\\htdocs")
        with open(f"C:\\xampp\\htdocs\\{filename}", 'r') as csvfile:
            x = []
            y = []
            plots = list(csv.reader(csvfile, delimiter=','))
            # print(list(plots))
            for i, row in enumerate(plots[:-3]):
                y.append(float(row[0]))
                x.append(i)

        plt.plot(x, y, color='g', label="ciśnienie")
        plt.xlabel('czas')
        plt.ylabel('ciśnienie')
        plt.axhline(y=float(plots[-2][0]), color='r', linestyle='--')
        plt.axhline(y=float(plots[-1][0]), color='b', linestyle='--')
        plt.title('Ciśnienie w czasie')
        plt.legend()
        plt.show()

        t = float(plots[-3][0])
        delta_p = float(plots[-1][0]) - float(plots[-2][0])
        # h = delta_p / 0.095
        g = 9.81
        print(t)
        print(delta_p)
        real_h = (g * t * t)/2
        print(real_h)
        wsp = delta_p/real_h

        print(f"Prawidłowy współczynnik powinien wynosić {wsp}")

    sleep(0.5)
