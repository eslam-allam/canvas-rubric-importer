APP_NAME=canvas-rubric-gui
VENV=.venv
PYTHON=$(VENV)/bin/python
PIP=$(VENV)/bin/pip
PYINSTALLER=$(VENV)/bin/pyinstaller
DIST_DIR=dist/$(APP_NAME)
INSTALL_PREFIX=/opt/$(APP_NAME)
DESKTOP_FILE=$(HOME)/.local/share/applications/$(APP_NAME).desktop
ICON_NAME=$(APP_NAME)
ICON_SRC=icons/$(APP_NAME).png
ICON_DEST=/usr/share/pixmaps/$(APP_NAME).png

.PHONY: all venv build install clean distclean

all: build

venv:
	python3 -m venv $(VENV)
	$(PIP) install --upgrade pip
	$(PIP) install requests pyinstaller

build: venv
	$(PYINSTALLER) --noconfirm --windowed --name $(APP_NAME) gui_app.py

install: build
	sudo mkdir -p $(INSTALL_PREFIX)
	sudo cp -r $(DIST_DIR)/* $(INSTALL_PREFIX)/
	@if [ -f "$(ICON_SRC)" ]; then \
		sudo mkdir -p $$(dirname "$(ICON_DEST)"); \
		sudo cp "$(ICON_SRC)" "$(ICON_DEST)"; \
	fi
	mkdir -p $$(dirname "$(DESKTOP_FILE)")
	cp packaging/$(APP_NAME).desktop "$(DESKTOP_FILE)"
	chmod +x "$(DESKTOP_FILE)"

clean:
	rm -rf build dist *.spec

distclean: clean
	rm -rf $(VENV)
