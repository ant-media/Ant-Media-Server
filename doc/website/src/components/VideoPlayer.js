import React from 'react';
import clsx from 'clsx';

export default function VideoPlayer({children, video, youtube}){

    if(youtube == "true"){
        return (
            <iframe className="border border-rounded m-3" width="100%" height="450" src={video} title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
        )
    } else {
        return (
            <div>Not implemented any custom player yet.</div>
        )
    }

}