#!/bin/bash
# Run a command with calendar timestamps on stderr. Keep stdout unchanged.
# Supervise both processes so the last stderr messages are drained before exit.

if [[ $# -eq 0 ]]; then
    echo "Usage: $0 command [argument ...]" >&2
    exit 2
fi

timestamp_stderr() {
    local line instant fraction timestamp
    while IFS= read -r line || [[ -n "$line" ]]; do
        if [[ -n "${EPOCHREALTIME:-}" ]]; then
            # Bash 5 can format milliseconds without spawning date for every line.
            instant=$EPOCHREALTIME
            fraction=${instant#*.}
            printf -v timestamp '%(%Y-%m-%d %H:%M:%S)T' "${instant%.*}"
            timestamp+=",${fraction:0:3}"
        else
            # Bash 4 is still shipped by some supported Linux installations.
            timestamp=$(date '+%Y-%m-%d %H:%M:%S,%3N') || return
        fi
        printf '%s %s\n' "$timestamp" "$line" || return
    done
}

command_pid=
stop_signal=
wait_interrupted=false

forward_signal() {
    stop_signal=$1
    wait_interrupted=true
    if [[ -n "$command_pid" ]]; then
        kill -s "$stop_signal" "$command_pid" 2>/dev/null || :
    fi
}

wait_for_process() {
    local status
    while :; do
        wait_interrupted=false
        wait "$1"
        status=$?
        # A trapped signal interrupts Bash's wait before the child has exited.
        if [[ "$wait_interrupted" == false ]]; then
            return "$status"
        fi
    done
}

trap 'forward_signal TERM' TERM
# Background commands inherit SIGINT ignored; translate Ctrl-C into SIGTERM.
trap 'forward_signal TERM' INT
trap 'forward_signal HUP' HUP

# The helper writes to the original stderr, including systemd's configured file.
# Ignore stop signals in the helper: EOF from Java ends it after draining output.
exec {stderr_fd}> >(trap '' TERM INT HUP; timestamp_stderr >&2)
logger_pid=$!

"$@" <&0 2>&"$stderr_fd" {stderr_fd}>&- &
command_pid=$!
exec {stderr_fd}>&-

# Also handle a stop signal received just before command_pid was assigned.
if [[ -n "$stop_signal" ]]; then
    kill -s "$stop_signal" "$command_pid" 2>/dev/null || :
fi

wait_for_process "$command_pid"
command_status=$?
command_pid=
wait_for_process "$logger_pid"
logger_status=$?

if [[ "$command_status" -ne 0 ]]; then
    exit "$command_status"
fi
exit "$logger_status"
