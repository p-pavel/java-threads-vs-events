#!/bin/sh

if uname | grep Darwin > /dev/null
then
  TOP_FLAGS="-pid"
else 
  TOP_FLAGS="-H -p"
fi


do_measure()  {
  MODE=$1
  PAUSE_BEFORE_PROFILING=120
  NUM_STREAMS=400
  FNAME="result-${MODE}_${NUM_STREAMS}"
  echo Running $MODE, streams=$NUM_STREAMS, file names $FNAME
  mkfifo $FNAME.fifo
  java -Xss300k -XX:+UnlockDiagnosticVMOptions -verbose:gc -XX:+PrintCompilation -XX:+DebugNonSafepoints -jar socket_test $NUM_STREAMS $MODE > $FNAME.fifo &
  PID=$!
  cat $FNAME.fifo | tee $FNAME.out &
  echo Started proces $PID, waiting $PAUSE_BEFORE_PROFILING seconds before profiling
  sleep $PAUSE_BEFORE_PROFILING
  top $TOP_FLAGS $PID > $FNAME.top
#  profiler/profiler.sh $PID > "$FNAME.profile"
  cleanup
}
cleanup() {
  kill -KILL $PID
  rm -f $FNAME.fifo
}
trap "echo 'Terminating background processes...'; cleanup; exit 1" SIGINT SIGTERM EXIT
for name in fs2 native loom
do
 do_measure $name
done
