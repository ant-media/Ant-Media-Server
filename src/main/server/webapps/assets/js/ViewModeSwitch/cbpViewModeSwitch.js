/**
 * cbpViewModeSwitch.js v1.0.0
 * http://www.codrops.com
 *
 * Licensed under the MIT license.
 * http://www.opensource.org/licenses/mit-license.php
 * 
 * Copyright 2013, Codrops
 * http://www.codrops.com
 */


function switcher() {



    var container = document.getElementById('cbp-vm'),
        optionSwitch = Array.prototype.slice.call(container.querySelectorAll('div.cbp-vm-options > a'));

        optionSwitch.forEach(function (el, i) {
            el.addEventListener('click', function (ev) {
                ev.preventDefault();

                _switch(this);


            }, false);
        });

    function _switch(opt) {
        // remove other view classes and any selected option
        optionSwitch.forEach(function (el) {
            classie.remove(container, el.getAttribute('data-view'));
            classie.remove(el, 'cbp-vm-selected');
        });
        // add the view class for this option
        classie.add(container, opt.getAttribute('data-view'));
        // this option stays selected
        classie.add(opt, 'cbp-vm-selected');
    }




}

