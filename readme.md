# SoC Now Generator
A mini SoC Generator based on CHISEL and Scala

In depth details of Generator is [here](https://github.com/merledu/SoC-Now-Generator/blob/main/overview.md) .

This repo is sub-repo of [SoC-Now](https://github.com/merledu/SoC-Now) which provides a Graphical Web Interface for the SoC Generator in this repo.

### Architecture
| Module  | Purpose |
| ------------- | ------------- |
| [NucleusRV](https://github.com/merledu/caravan)  | Parametrized Configurable Core  |
| [Caravan](https://github.com/merledu/caravan)  | Bus Interconnects  |
| [Jigsaw](https://github.com/merledu/jigsaw)  | Devices / Peripherals  |
| [CCache](https://github.com/shahzaibk23/ccache)  | Caches  |
