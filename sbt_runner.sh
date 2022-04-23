
for i in {1..4}
do
    echo $i
    python3 soc_test.py $i
    sbt test
done