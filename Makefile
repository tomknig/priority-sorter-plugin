DOCKER_COMMAND=docker run -it --rm --name priority-sorter -v $(PWD):/usr/src/priority-sorter -v "${HOME}/.m2":/root/.m2 --env-file ./maven.env -w /usr/src/priority-sorter maven:3-jdk-8

export MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

build: ## build priority sorter plugn
	$(DOCKER_COMMAND) mvn install

plugin: ## create jenkins plugin archetype skeleton
	$(DOCKER_COMMAND) mvn archetype:generate -Dfilter="io.jenkins.archetypes:"

test: ## package priority sorter plugin
	rm -rf target/*
	$(DOCKER_COMMAND) mvn -P enable-jacoco -Dmaven.spotbugs.skip=true test verify

package: ## package priority sorter plugin
	rm -rf target/*
	$(DOCKER_COMMAND) mvn -P quick-build -Dmaven.spotbugs.skip=true -Dmaven.test.skip=true versions:use-latest-versions package
	cat target/classes/META-INF/annotations/hudson.Extension.txt
	cp target/PrioritySorter.hpi ../jocker/plugins/PrioritySorter.jpi 

spotbugs: ## package priority sorter plugin
	$(DOCKER_COMMAND) mvn -Dmaven.test.skip=true install spotbugs:check
	#spotbugs:gui
