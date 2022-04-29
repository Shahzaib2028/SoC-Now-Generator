
for i in {0..511}
do
    echo $i
    python3 soc_test.py $i
    python3 peripheralScript.py
    make bitstream
done