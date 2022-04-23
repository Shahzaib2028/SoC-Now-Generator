import subprocess,os,json
from unittest import TestCase

class SoC_Tester(TestCase):

    def test_soc_combination(self):
        # assert True
        # os.chdir("../../")
        print(f"CURRENT DIR = {os.system('pwd')}")
        defaults = {"i": 1, "gpio": 1}
        extensions = {"m": 1, "f": 0, "c": 0}
        devices = {"spi": 0, "uart": 0, "timer": 0, "spi_flash": 1, "i2c": 0}
        bus = {"wb": 0, "tl": 0}
        total_combinations = 2**len(extensions.keys()) * 2**len(devices.keys()) * len(bus.keys())
        print(f"\n Total Combinations: {total_combinations}\n")
        comb_count = 0
        import itertools
        from typing import final
        device_combs = list(itertools.product([0, 1], repeat=len(devices.keys())))
        extension_combs = list(itertools.product([0, 1], repeat=len(extensions.keys())))
        bus_combs = [ [0]*len(bus.keys())  for i,v in enumerate(bus.keys())]
        for i,v in enumerate(bus_combs):
            v[i] = 1

        for i,v in enumerate(bus_combs):
            # current bus comb
            final_dict = dict(zip(bus.keys(), v))
            if comb_count == 3:
                break

            for j,w in enumerate(extension_combs):
                # current ext comb
                final_dict |= dict(zip(extensions.keys(), w))
                if comb_count == 3:
                    break

                for k,x in enumerate(device_combs):
                    if comb_count == 3:
                        break

                    # current dev comb
                    final_dict |= dict(zip(devices.keys(), x))

                    final_dict |= defaults
                    # attach code here
                    comb_count+=1
                    print(f"\nCurrent Combination #{comb_count} : {final_dict}\n")
                    file = open("src/main/scala/config.json","w")
                    json.dump(final_dict, file)
                    file.close()

                    process = subprocess.Popen(["sbt","test"], stdout=subprocess.PIPE)
                    while True:
                        output = process.stdout.readline()
                        if output == '' and process.poll() is not None:
                            break
                        if output:
                            # print(str(output))
                            if "[info] All tests passed." in str(output):
                                print("TEST PASSED :)")
                                self.assertTrue(True)
                                break
                            if "TEST FAILED" in str(output):
                                print("TEST FAILES :(")
                                self.assertTrue(False)
                                break



