const postcss = require('postcss');

// Filters out any non-color CSS declarations

function hasParent(decl, predicate) {
	let parent = decl.parent;
	while (parent) {
		if (predicate(parent)) return true;
		parent = parent.parent;
	}
	return false;
}

module.exports = postcss.plugin('postcss-only-colors', () => {
	return (root) => {
		root.walkDecls(decl => {
			if (hasParent(decl, (parent) => parent.type === 'atrule' && (parent.name === 'svg-load'))) {
				return;
			}
			if (decl.prop.match('background')
					|| decl.prop.match('border') && decl.prop !== 'border-radius' && decl.prop !== 'border-spacing'
					|| decl.prop === 'box-shadow'
					|| decl.prop === 'text-shadow'
			) {
				return;
			}
			const colorRegex = /#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})?/g;
			const hasColor = colorRegex.exec(decl.value);
			if (!hasColor) {
				decl.remove();
			}
		});
	};
});