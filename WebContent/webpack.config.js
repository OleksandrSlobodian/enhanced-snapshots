const path = require('path');
const webpack = require('webpack');
const autoprefixer = require('autoprefixer');
const ExtractTextPlugin = require("extract-text-webpack-plugin");

module.exports = {
    //context: path.resolve(__dirname, 'WebContent'),
    entry: {
        vendor: Object.keys(require('./package.json').dependencies),
        esnap: './js/app.js'
    },
    devtool: 'inline-source-map',
    output: {
        path: path.resolve(__dirname, 'dist'),
        publicPath: "/",
        filename: '[name].min.js'
    },
    node: {
        fs: "empty",
        net: "empty",
        tls: "empty"
    },

    watchPoll: true,
    watchOptions: {
        ignored: /node_modules/,
        aggregateTimeout: 300,
        poll: 500
    },
    devServer: {
        watchPoll: true,
        port: 5000,
        watchOptions: {
            ignored: /node_modules/,
            aggregateTimeout: 300,
            poll: 500
        }
    },
    resolve: {
        modulesDirectories: [ 'node_modules'],
        extensions: [ '', '.js', '.css', '.json' ]
    },

    module: {
        preLoaders: [
            { test: /\.json$/, loader: 'json'}
        ],
        loaders: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                loader: 'babel-loader'
            },
            {
                test: /\.html$/,
                loader: 'raw'
            },
            {
                test: /\.css$/,
                loader: ExtractTextPlugin.extract('style', 'css-loader!postcss-loader')
            },
            {
                test: /\.png$/,
                loader: "url-loader?mimetype=image/png"
            }
        ]
    },
    plugins: [
        new ExtractTextPlugin('[name].styles.css'),
        new webpack.ProvidePlugin({
            //'_': 'lodash',
            '$': 'jquery',
            'jQuery': 'jquery',
            'window.jQuery': 'jquery'
        })
    ],
    'postcss': [
        autoprefixer({
            browsers: [
                'last 2 versions',
                'Explorer >= 11'
            ],
            cascade: false
        })
    ]
};
