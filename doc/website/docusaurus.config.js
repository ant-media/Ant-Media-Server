// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Ant Media Documentation',
  tagline: 'Ant Media Documentation',
  url: 'https://timantmedia.github.io',
  baseUrl: '/docs',
  trailingSlash: true,
  onBrokenLinks: 'ignore',
  onBrokenMarkdownLinks: 'ignore',
  favicon: 'img/favicon.ico',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'timantmedia', // Usually your GitHub org/user name.
  projectName: 'resources', // Usually your repo name.
  deploymentBranch: 'master', // The branch that GitHub pages deploys from.

  // Even if you don't use internalization, you can use this field to set useful
  // metadata like html lang. For example, if your site is Chinese, you may want
  // to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          routeBasePath: '/',
          sidebarPath: require.resolve('./sidebars.js'),
          breadcrumbs: true,
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl: 'https://github.com/timantmedia/resources/tree/master/docs',
        },
        blog: {
          showReadingTime: true,
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/facebook/docusaurus/tree/main/packages/create-docusaurus/templates/shared/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  plugins: [[ require.resolve('docusaurus-lunr-search'), {
    languages: ['en']
  }]],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      navbar: {
        title: 'Ant Media Documentation',
        logo: {
          alt: 'Ant Media Logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'doc',
            docId: 'introduction',
            label: 'Guides',
          },
          {
            label: 'SDK references',
            href: '/sdk-reference'
          },
          {
            label: 'Blog',
            href: 'https://antmedia.io/blog/'
          }
          /*{to: '/blog', label: 'Blog', position: 'left'},
          {
            href: 'https://github.com/facebook/docusaurus',
            label: 'GitHub',
            position: 'right',
          },*/
        ],
      },
      colorMode: {
        defaultMode: 'light',
        disableSwitch: false,
        respectPrefersColorScheme: false,
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Sample Applications',
                to: '/get-started/Sample-tools-and-applications/',
              },
              {
                label: 'Clustering & Scaling',
                to: '/category/clustering-and-scaling/',
              },
              {
                label: 'Adaptive Bitrate',
                to: '/category/adaptive-bitrate/',
              },
              {
                label: 'Troubleshooting',
                to: '/guides/troubleshooting/',
              },
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'Developer Community',
                href: 'https://community.antmedia.io/',
              },
              {
                label: 'Stackoverflow',
                href: 'https://stackoverflow.com/questions/tagged/ant-media-server',
              },
              {
                label: 'GitHub',
                href: 'https://github.com/ant-media',
              },
              {
                label: 'Youtube',
                href: 'https://www.youtube.com/@AntMediaServer/',
              },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'My Account',
                href: 'https://antmedia.io/my-account/',
              },
              {
                label: 'Support Packages',
                href: 'https://antmedia.io/pricing/support-packages/',
              },
              {
                label: 'Blog',
                to: 'https://antmedia.io/blog/',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} Ant Media`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
    }),
};

module.exports = config;
