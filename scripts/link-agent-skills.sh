#!/usr/bin/env bash
# Łączy oficjalne skille z third_party/android-skills do katalogów agentów
# oraz kopiuje/linkuje własne skille z .cursor/skills do .claude/skills.
# Domyślnie symlinki. Na Windowsie bez uprawnień do symlinków: --copy
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
mode="symlink"
if [[ "${1:-}" == "--copy" ]]; then
  mode="copy"
elif [[ -n "${1:-}" ]]; then
  echo "Użycie: $0 [--copy]" >&2
  exit 2
fi

vendor="$root/third_party/android-skills"
if [[ ! -d "$vendor" ]]; then
  echo "Brak snapshotu: $vendor" >&2
  exit 1
fi

relpath() {
  python3 -c 'import os,sys; print(os.path.relpath(sys.argv[1], sys.argv[2]))' "$1" "$2"
}

link_upstream() {
  local dest_dir="$1"
  local name="$2"
  local target="$3"
  mkdir -p "$dest_dir"
  local dest="$dest_dir/$name"
  if [[ -L "$dest" ]]; then
    rm "$dest"
  elif [[ -e "$dest" ]]; then
    echo "Pomijam (istnieje i nie jest symlinkiem): $dest" >&2
    return
  fi
  if [[ "$mode" == "copy" ]]; then
    cp -a "$target" "$dest"
  else
    ln -s "$(relpath "$target" "$dest_dir")" "$dest"
  fi
}

while IFS= read -r skill_md; do
  src="$(dirname "$skill_md")"
  name="$(basename "$src")"
  for agent_dir in "$root/.cursor/skills" "$root/.claude/skills"; do
    link_upstream "$agent_dir" "$name" "$src"
  done
done < <(find "$vendor" -name SKILL.md -print)

shopt -s nullglob
for src in "$root/.cursor/skills"/*; do
  [[ -d "$src" && ! -L "$src" ]] || continue
  [[ -f "$src/SKILL.md" ]] || continue
  name="$(basename "$src")"
  dest="$root/.claude/skills/$name"
  if [[ -L "$dest" ]]; then
    rm "$dest"
  elif [[ -e "$dest" ]]; then
    rm -rf "$dest"
  fi
  if [[ "$mode" == "copy" ]]; then
    cp -a "$src" "$dest"
  else
    ln -s "../../.cursor/skills/$name" "$dest"
  fi
done

echo "Skille podpięte ($mode)."
