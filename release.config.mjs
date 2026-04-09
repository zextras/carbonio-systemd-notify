// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

/**
 * @type {import('semantic-release').GlobalConfig}
 */
export default {
  branches: ['main'],
  tagFormat: "${version}",
  plugins: [
    [
      '@semantic-release/commit-analyzer',
      {
        preset: 'conventionalcommits',
        releaseRules: [
          { type: 'refactor', release: 'patch' },
          { type: 'build', release: 'patch' },
          { type: 'ci', release: 'patch' },
          { type: 'chore', scope: 'release', release: false },
          { type: 'perf', release: 'patch' }
        ]
      }
    ],
    [
      '@semantic-release/release-notes-generator',
      {
        preset: 'conventionalcommits',
        presetConfig: {
          types: [
            { type: 'feat', section: 'Features', hidden: false },
            { type: 'fix', section: 'Bug Fixes', hidden: false },
            { type: 'refactor', section: 'Other changes', hidden: false },
            { type: 'build', section: 'Other changes', hidden: false },
            { type: 'ci', section: 'Other changes', hidden: false }
          ]
        }
      }
    ],
    [
      '@semantic-release/exec',
      {
        prepareCmd: "sed -i '0,/<version>.*-SNAPSHOT<\\/version>/s|<version>.*-SNAPSHOT</version>|<version>${nextRelease.version}</version>|' pom.xml"
      }
    ],
    [
      '@semantic-release/git',
      {
        assets: ['pom.xml'],
        message: 'chore(release): ${nextRelease.version} [skip ci]'
      }
    ],
    '@semantic-release/github'
  ]
};
