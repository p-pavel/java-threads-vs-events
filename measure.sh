#!/bin/sh

PAUSE_BEFORE_PROFILING=20
NUM_STREAMS=400

if uname | grep Darwin > /dev/null
then
  TOP_FLAGS="-pid"
else 
  TOP_FLAGS="-H -p"
fi

collectTopInfo() {
  local TOP_PID
  top $TOP_FLAGS $PID > $FNAME.top &
  TOP_PID=$!
  sleep 5
  kill $TOP_PID > /dev/null
}

do_measure()  {
  MODE=$1
  FNAME="result-${MODE}_${NUM_STREAMS}"
  echo Running $MODE, streams=$NUM_STREAMS, file names $FNAME
  mkfifo $FNAME.fifo
  java -Xss300k -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -jar socket_test $NUM_STREAMS $MODE > $FNAME.fifo &
  PID=$!
  cat $FNAME.fifo | tee $FNAME.out &
  echo "Started proces $PID, waiting $PAUSE_BEFORE_PROFILING seconds before profiling"
  sleep $PAUSE_BEFORE_PROFILING
  echo "Storing process TOP output and starting profiling"
  collectTopInfo
  echo "Starting profiler"
  # profiler/profiler.sh $PID > "$FNAME.profile"
  echo "Done profiling, killing process $PID"
  cleanup
}

cleanup() {
  kill -KILL $PID > /dev/null
  rm -f $FNAME.fifo
}

trap "echo 'Terminating background processes...'; cleanup; exit 1" SIGINT SIGTERM EXIT

for name in fs2 native loom io
do
 do_measure $name
done