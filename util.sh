# bash
function cldr-tool() {
    CLDRCLASS=$1
    if [[ "" == $(echo "${CLDRCLASS}" | tr -c -d .) ]];
    then
        # default package
        CLDRCLASS=org.unicode.cldr.tool.${CLDRCLASS}
    fi
    shift
    EXECARGS="${1}"
    shift
    echo ${CLDRCLASS} "${EXECARGS}"  -Dexec.cleanupDaemonThreads=false  ${@}
    env LANG=en_US.UTF-8 mvn  -DCLDR_DIR=$(pwd) -DCLDR_GITHUB_ANNOTATIONS=true --file=tools/pom.xml -pl cldr-code ${JVMARGS} \
         -Dfile.encoding=utf-8 compile -DskipTests=true exec:java -Dexec.mainClass=${CLDRCLASS} \
         -Dexec.args="${EXECARGS}"  -Dexec.cleanupDaemonThreads=false ${@}
}

cldr-tool GenerateProductionData