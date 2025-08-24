const postcss = require('postcss');

// Filters out any non-color CSS declarations

module.exports = postcss.plugin('postcss-only-colors', () => {
	return (root) => {
		root.walkDecls(decl => {
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