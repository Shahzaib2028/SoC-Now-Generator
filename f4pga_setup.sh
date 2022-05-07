wget https://repo.continuum.io/miniconda/Miniconda3-latest-Linux-x86_64.sh -O conda_installer.sh
export INSTALL_DIR=~/opt/f4pga
export FPGA_FAM=xc7
bash conda_installer.sh -u -b -p $INSTALL_DIR/$FPGA_FAM/conda;
source "$INSTALL_DIR/$FPGA_FAM/conda/etc/profile.d/conda.sh";
conda env create -f $INSTALL_DIR/$FPGA_FAM/environment.yml
mkdir -p $INSTALL_DIR/xc7/install
wget -qO- https://storage.googleapis.com/symbiflow-arch-defs/artifacts/prod/foss-fpga-tools/symbiflow-arch-defs/continuous/install/20220406-185509/symbiflow-arch-defs-install-3ef4188.tar.xz | tar -xJC $INSTALL_DIR/xc7/install
wget -qO- https://storage.googleapis.com/symbiflow-arch-defs/artifacts/prod/foss-fpga-tools/symbiflow-arch-defs/continuous/install/20220406-185509/symbiflow-arch-defs-xc7a50t_test-3ef4188.tar.xz | tar -xJC $INSTALL_DIR/xc7/install
wget -qO- https://storage.googleapis.com/symbiflow-arch-defs/artifacts/prod/foss-fpga-tools/symbiflow-arch-defs/continuous/install/20220406-185509/symbiflow-arch-defs-xc7a100t_test-3ef4188.tar.xz | tar -xJC $INSTALL_DIR/xc7/install
wget -qO- https://storage.googleapis.com/symbiflow-arch-defs/artifacts/prod/foss-fpga-tools/symbiflow-arch-defs/continuous/install/20220406-185509/symbiflow-arch-defs-xc7a200t_test-3ef4188.tar.xz | tar -xJC $INSTALL_DIR/xc7/install
wget -qO- https://storage.googleapis.com/symbiflow-arch-defs/artifacts/prod/foss-fpga-tools/symbiflow-arch-defs/continuous/install/20220406-185509/symbiflow-arch-defs-xc7z010_test-3ef4188.tar.xz | tar -xJC $INSTALL_DIR/xc7/install
export PATH="$INSTALL_DIR/$FPGA_FAM/install/bin:$PATH";
source "$INSTALL_DIR/$FPGA_FAM/conda/etc/profile.d/conda.sh"
conda activate $FPGA_FAM
