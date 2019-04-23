# Makefile for Clojure services

# debug or release
PROFILE=debug
LEIN_ENV=
LEIN=$(LEIN_ENV) lein
# Optional list of tasks for worker
TASKS=
# Optional test taget
TEST=

.PHONY: all clean deps doc repl run run-once service test unit-test install.sh uberjar

all: service unit-test classpath

clean: 
	rm -rf target

deps:
	bash -c 'cd checkouts; for dir in `ls .`; do pushd $$dir; $(LEIN) install; popd; done'
	$(LEIN) with-profile "$(PROFILE)" deps

repl:
	$(LEIN) with-profile "$(PROFILE)" repl

run: 
	java -server \
		-cp $$($(LEIN) with-profile "$(PROFILE)" classpath) \
		-Dclojure.compiler.direct-linking=true \
		clojure.main -m clubzz.data.main

service: deps

test:
	$(LEIN) with-profile "$(PROFILE)" test $(TEST)

unit-test:
	$(LEIN) with-profile "$(PROFILE)" test :unit

classpath:
	$(LEIN) with-profile "$(PROFILE)" classpath > classpath

uberjar: 
	$(LEIN) with-profile "$(PROFILE)" uberjar
