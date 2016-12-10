`sbt clean run`

# Digital Ocean Notes:

```
    1  cd
    2  mkdir bin
    3  cd bin
    4  wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.13/sbt-0.13.13.tgz
    5  clear
    6  ls
    7  tar -xvzf sbt-0.13.13.tgz
    8  cd ..
    9  mkdir workspace
   10  cd workspace
   11  git clone https://github.com/kevinmeredith/twitter_streaming_client.git
   12  cd twitter_streaming_client/
   13  ~/bin/sbt-launcher-packaging-0.13.13/bin/sbt run
   15  sudo apt-get install openjdk-8-jdk
   16  clear
   17  ~/bin/sbt-launcher-packaging-0.13.13/bin/sbt run
```