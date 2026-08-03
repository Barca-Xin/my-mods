#!/usr/bin/env python3
"""把后端 SQLite 数据导出成静态站需要的 data.json，并把模组 jar 拷到前端 public/files/。

用于 GitHub Pages 静态部署：发布新模组/版本后，重新跑一次本脚本再 push 即可。
用法：python scripts/export_data.py
"""
import json
import shutil
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DB = ROOT / 'backend' / 'data' / 'mods.db'
UPLOADS = ROOT / 'backend' / 'uploads'
PUBLIC = ROOT / 'frontend' / 'public'
FILES_DST = PUBLIC / 'files'


def main() -> None:
    if not DB.exists():
        print(f'找不到数据库 {DB}，请先启动过后端再导出')
        return

    con = sqlite3.connect(str(DB))
    con.row_factory = sqlite3.Row
    cur = con.cursor()

    mods = cur.execute(
        'SELECT id, slug, name, logo_url, short_desc, category, mod_loader,'
        '       source_code_url, wiki_url'
        '  FROM mods ORDER BY id').fetchall()

    out_mods = []
    for m in mods:
        versions = cur.execute(
            'SELECT v.id, v.game_version, v.mod_version, v.file_size, v.md5,'
            '       v.changelog, v.release_date, v.is_recommended, v.download_count,'
            '       f.bucket_path'
            '  FROM mod_versions v'
            '  LEFT JOIN file_records f ON f.id = v.file_record_id'
            ' WHERE v.mod_id = ?'
            ' ORDER BY v.release_date DESC, v.id DESC', (m['id'],)).fetchall()

        deps = cur.execute(
            'SELECT name, depends_on_mod_id FROM mod_dependencies WHERE mod_id = ?',
            (m['id'],)).fetchall()

        version_list = []
        for v in versions:
            url = None
            if v['bucket_path']:
                url = 'files/' + v['bucket_path'].replace('\\', '/')
            version_list.append({
                'id': v['id'],
                'gameVersion': v['game_version'],
                'modVersion': v['mod_version'],
                'fileSize': v['file_size'],
                'md5': v['md5'],
                'changelog': v['changelog'],
                'releaseDate': v['release_date'],
                'recommended': bool(v['is_recommended']),
                'downloadUrl': url,
                'downloadCount': v['download_count'] or 0,
            })

        dep_names = []
        for d in deps:
            if d['depends_on_mod_id']:
                row = cur.execute('SELECT slug FROM mods WHERE id = ?',
                                  (d['depends_on_mod_id'],)).fetchone()
                dep_names.append(row['slug'] if row else d['name'])
            else:
                dep_names.append(d['name'])

        out_mods.append({
            'modId': m['id'],
            'slug': m['slug'],
            'name': m['name'],
            'logoUrl': m['logo_url'],
            'shortDesc': m['short_desc'],
            'category': m['category'],
            'modLoader': m['mod_loader'],
            'sourceCodeUrl': m['source_code_url'],
            'wikiUrl': m['wiki_url'],
            'dependencies': dep_names,
            'versions': version_list,
        })

    # 拷贝 jar 文件到 public/files/
    if FILES_DST.exists():
        shutil.rmtree(FILES_DST)
    jar_count = 0
    src_mods = UPLOADS / 'mods'
    if src_mods.exists():
        for jar in src_mods.rglob('*.jar'):
            rel = jar.relative_to(UPLOADS).as_posix()
            dst = FILES_DST / rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(jar, dst)
            jar_count += 1

    PUBLIC.mkdir(exist_ok=True)
    (PUBLIC / 'data.json').write_text(
        json.dumps({'mods': out_mods}, ensure_ascii=False, indent=2), encoding='utf-8')

    print(f'导出完成：{len(out_mods)} 个模组，{jar_count} 个 jar 文件')
    print(f'data.json → {PUBLIC / "data.json"}')


if __name__ == '__main__':
    main()
