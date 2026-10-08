#!/bin/sh
# Flatpak launcher: forwards all arguments to the bundled jlink launcher,
# so both GUI (no args) and CLI (--cli ...) work.
exec /app/lib/canvas-rubric-importer/bin/CanvasRubricImporter "$@"
