import React from 'react';
// Import the original mapper
import MDXComponents from '@theme-original/MDXComponents';
import VideoPlayer from '@site/src/components/VideoPlayer';
import InfoBox from '@site/src/components/InfoBox';

export default {
  // Re-use the default mapping
  ...MDXComponents,
  // Map the "highlight" tag to our <Highlight /> component!
  // `Highlight` will receive all props that were passed to `highlight` in MDX
  VideoPlayer: VideoPlayer,
  InfoBox: InfoBox,
};