wget https://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh -O conda_installer.sh
bash conda_installer.sh -u -b -p ~/opt/f4pga/xc7/conda;
source "~/opt/f4pga/xc7/conda/etc/profile.d/conda.sh";
conda env create -f ~/opt/f4pga/xc7/install/environment.yml
mkdir -p ~/opt/f4pga/xc7/install
wget -qO- https://storage.googleapis.com/symbiflow-arch-defs/artifacts/prod/foss-fpga-tools/symbiflow-arch-defs/continuous/install/20220406-185509/symbiflow-arch-defs-install-3ef4188.tar.xz | tar -xJC ~/opt/f4pga/xc7/install
wget -qO- https://storage.googleapis.com/symbiflow-arch-defs/artifacts/prod/foss-fpga-tools/symbiflow-arch-defs/continuous/install/20220406-185509/symbiflow-arch-defs-xc7a50t_test-3ef4188.tar.xz | tar -xJC ~/opt/f4pga/xc7/install
wget -qO- https://storage.googleapis.com/symbiflow-arch-defs/artifacts/prod/foss-fpga-tools/symbiflow-arch-defs/continuous/install/20220406-185509/symbiflow-arch-defs-xc7a100t_test-3ef4188.tar.xz | tar -xJC ~/opt/f4pga/xc7/install
wget -qO- https://storage.googleapis.com/symbiflow-arch-defs/artifacts/prod/foss-fpga-tools/symbiflow-arch-defs/continuous/install/20220406-185509/symbiflow-arch-defs-xc7a200t_test-3ef4188.tar.xz | tar -xJC ~/opt/f4pga/xc7/install
wget -qO- https://storage.googleapis.com/symbiflow-arch-defs/artifacts/prod/foss-fpga-tools/symbiflow-arch-defs/continuous/install/20220406-185509/symbiflow-arch-defs-xc7z010_test-3ef4188.tar.xz | tar -xJC ~/opt/f4pga/xc7/install
export PATH="~/opt/f4pga/xc7/install/bin:$PATH";
source "~/opt/f4pga/xc7/conda/etc/profile.d/conda.sh"
conda activate xc7
